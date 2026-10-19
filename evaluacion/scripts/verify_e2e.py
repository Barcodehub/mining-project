"""
Verificación funcional de punta a punta del sistema v2 (requiere el stack de docker compose arriba).

Comprueba, contra los servicios reales:
  1. Ingesta de una fila real de la CDMB (quebrada La Baja, ene-2025) con un adapter
     heterogéneo: Hg "<0.0005 mg/L" censurado, CN 3.14 mg/L → nivel CRITICO.
  2. La alerta viaja por Kafka (environmental-alerts) y queda en la auditoría y en las
     notificaciones del destinatario configurado.
  3. Una medición NORMAL no genera alerta.
  4. La métrica alert_e2e_latency_seconds se expone en Prometheus.
  5. Un reporte ciudadano se audita con seudónimo (sin userId) y un usuario CLIENTE
     ve la ubicación generalizada en cuadrícula.

Uso:  python evaluacion/scripts/verify_e2e.py [--mgmt http://localhost:8082] [--audit http://localhost:8083]
"""
import argparse
import json
import sys
import time
import uuid

import requests


def ok(cond, msg):
    print(("  [OK]   " if cond else "  [FAIL] ") + msg)
    return bool(cond)


def login(mgmt, email, password):
    r = requests.post(f"{mgmt}/api/v1/auth/login", json={"email": email, "password": password}, timeout=10)
    r.raise_for_status()
    b = r.json()
    return b.get("token") or (b.get("data") or {}).get("token")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mgmt", default="http://localhost:8082")
    ap.add_argument("--audit", default="http://localhost:8083")
    ap.add_argument("--users", default="http://localhost:8081")
    a = ap.parse_args()
    results = []

    token = login(a.mgmt, "admin@admin.com", "Admin123")
    H = {"Authorization": f"Bearer {token}"}
    print("1. Ingesta de fila real CDMB (La Baja) con adapter heterogéneo")
    mapping = {
        "device_id": "c_digo_cdmb", "cluster_id": "cluster", "lat": "lat", "lng": "lon", "altitude": "altura",
        "timestamp": "fecha_y_hora_de_monitoreo", "ph": "ph_unidades_de_ph",
        "dissolved_oxygen": "oxigeno_disuelto_mg_l", "turbidity": "turbiedad_ntu", "temperature": "t_muestra_c",
        "hg_total": {"path": "mercurio_mg_l", "factor": 1000}, "cyanide": "cianuro_mg_l", "arsenic": "arsenico_mg_l",
    }
    r = requests.post(f"{a.mgmt}/api/v1/adapters", headers=H, json={"sensorType": "cdmb_open_data", "mapping": mapping}, timeout=10)
    results.append(ok(r.status_code == 201, f"adapter cdmb_open_data creado (HTTP {r.status_code})"))

    device = "QLB-01-" + uuid.uuid4().hex[:6]
    row = {"c_digo_cdmb": device, "cluster": "LA_BAJA", "lat": 7.3667, "lon": -72.8917, "altura": "2669",
           "fecha_y_hora_de_monitoreo": "2025-01-13T10:00:00.000", "ph_unidades_de_ph": "8.11",
           "oxigeno_disuelto_mg_l": "7.5", "turbiedad_ntu": "70", "t_muestra_c": "14.2",
           "mercurio_mg_l": "<0.0005", "cianuro_mg_l": "3.14", "arsenico_mg_l": "-999"}
    r = requests.post(f"{a.mgmt}/api/v1/environmental-data", headers=H,
                      json={"sensorType": "cdmb_open_data", "payload": row}, timeout=10)
    d = r.json()
    results.append(ok(r.status_code == 201, f"ingesta aceptada (HTTP {r.status_code}); el v1 la rechazaba por Hg < LD"))
    results.append(ok(d.get("alertLevel") == "CRITICO" and d.get("alertType") == "IMPACTO",
                      f"clasificada {d.get('alertLevel')}/{d.get('alertType')} — superaciones: {d.get('exceedances')}"))
    results.append(ok(d.get("censoredFields") == "hg_total<0.5", f"Hg censurado registrado: {d.get('censoredFields')}"))
    results.append(ok(d.get("thermalProfile") == "FRIA", f"perfil térmico por altitud: {d.get('thermalProfile')}"))
    mid = d.get("id")

    print("2. Alerta por Kafka → auditoría y notificación")
    found_audit = found_notif = None
    for _ in range(30):
        au = requests.get(f"{a.audit}/api/v1/audit", params={"action": "ENVIRONMENTAL_ALERT"}, timeout=10).json()
        au = au.get("data", au) if isinstance(au, dict) else au
        found_audit = next((x for x in au if x.get("entityId") == mid), None)
        no = requests.get(f"{a.audit}/api/v1/notifications/1", timeout=10).json()
        no = no.get("data", no) if isinstance(no, dict) else no
        found_notif = next((x for x in no if device in (x.get("message") or "")), None)
        if found_audit and found_notif:
            break
        time.sleep(0.5)
    results.append(ok(found_audit is not None, f"auditoría ENVIRONMENTAL_ALERT para la medición {mid}"))
    results.append(ok(found_notif is not None, f"notificación al destinatario 1: {found_notif and found_notif.get('type')}"))

    print("3. Medición NORMAL no genera alerta")
    normal = dict(row, c_digo_cdmb="AP-01-" + uuid.uuid4().hex[:6], cianuro_mg_l="<0.025", mercurio_mg_l="0.0003",
                  ph_unidades_de_ph="7.27", cluster="AGUA_PARAMO")
    r = requests.post(f"{a.mgmt}/api/v1/environmental-data", headers=H,
                      json={"sensorType": "cdmb_open_data", "payload": normal}, timeout=10).json()
    time.sleep(2)
    au = requests.get(f"{a.audit}/api/v1/audit", params={"action": "ENVIRONMENTAL_ALERT"}, timeout=10).json()
    au = au.get("data", au) if isinstance(au, dict) else au
    results.append(ok(r.get("alertLevel") == "NORMAL" and not any(x.get("entityId") == r.get("id") for x in au),
                      f"nivel {r.get('alertLevel')}, sin evento en la auditoría"))

    print("4. Métrica de latencia de punta a punta")
    prom = requests.get(f"{a.audit}/actuator/prometheus", timeout=10).text
    lines = [l for l in prom.splitlines() if l.startswith("alert_e2e_latency_seconds")]
    count = next((l for l in lines if l.startswith("alert_e2e_latency_seconds_count")), None)
    results.append(ok(count is not None, f"{count}"))
    q = [l for l in lines if 'quantile="0.5"' in l or 'quantile="0.99"' in l]
    for l in q:
        print("         " + l)

    print("5. Reporte ciudadano: seudónimo en auditoría y ubicación generalizada para CLIENTE")
    rep = {"title": "Prueba e2e", "content": "Agua turbia con olor químico", "userId": 1, "type": "REPORT",
           "latitude": 7.366712, "longitude": -72.891734, "clusterId": "LA_BAJA", "images": []}
    r = requests.post(f"{a.mgmt}/api/v1/posts", headers=H, json=rep, timeout=10)
    rid = r.json().get("id")
    time.sleep(2)
    au = requests.get(f"{a.audit}/api/v1/audit", params={"action": "REPORT_CREATED"}, timeout=10).json()
    au = au.get("data", au) if isinstance(au, dict) else au
    ev = next((x for x in au if x.get("entityId") == rid), None)
    results.append(ok(ev is not None and ev.get("userId") is None
                      and str((ev.get("metadata") or {}).get("reporterPseudonym", "")).startswith("u_"),
                      f"REPORT_CREATED sin userId y con seudónimo: {ev and ev.get('metadata')}"))

    email = f"ciudadano.{uuid.uuid4().hex[:8]}@test.co"
    reg = {"name": "Ciu", "lastName": "Dadano", "password": "ClaveSegura123", "email": email,
           "identificationDocument": str(int(time.time() * 1000))[-10:], "phoneNumber": "+573001112233",
           "birthDate": "1990-01-01", "roleId": 4}
    rr = None
    for path in ("/api/v1/users/public", "/users/public"):
        rr = requests.post(f"{a.users}{path}", json=reg, timeout=10)
        if rr.status_code in (200, 201):
            break
    if rr is not None and rr.status_code in (200, 201):
        ct = login(a.mgmt, email, "ClaveSegura123")
        lst = requests.get(f"{a.mgmt}/api/v1/reports", headers={"Authorization": f"Bearer {ct}"}, timeout=10).json()
        mine = next((x for x in lst if x.get("id") == rid), None)
        results.append(ok(mine is not None and mine.get("locationPrecision", "").startswith("GRID")
                          and mine.get("userId") is None and abs(mine.get("latitude") - 7.365) < 1e-9,
                          f"CLIENTE ve lat={mine and mine.get('latitude')} precision={mine and mine.get('locationPrecision')}"))
        adm = requests.get(f"{a.mgmt}/api/v1/reports", headers=H, timeout=10).json()
        mine = next((x for x in adm if x.get("id") == rid), None)
        results.append(ok(mine is not None and mine.get("locationPrecision") == "EXACT",
                          f"ADMINISTRADOR ve lat={mine and mine.get('latitude')} precision={mine and mine.get('locationPrecision')}"))
    else:
        results.append(ok(False, f"no se pudo registrar usuario CLIENTE (HTTP {rr and rr.status_code}: {rr and rr.text[:200]})"))

    passed = sum(results)
    print(f"\nResultado: {passed}/{len(results)} verificaciones correctas")
    sys.exit(0 if passed == len(results) else 1)


if __name__ == "__main__":
    main()
