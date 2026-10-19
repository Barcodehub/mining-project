package com.mining.soft.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Política del modelo de alerta (v2), cargada desde config-ambiental.yaml.
 * Cada factor declara su parámetro, dimensión, dirección y umbrales con fuente.
 */
@Configuration
@ConfigurationProperties(prefix = "ambiental")
@Getter @Setter
public class AmbientalConfig {

    public enum Dimension { HUMANA, ECOLOGICA, GEOQUIMICA }
    public enum TipoFactor { CONTAMINANTE, CONDICION }
    public enum Direccion { MAYOR, MENOR }
    public enum PerfilTermico { FRIA, CALIDA }

    private String region;
    private String version;

    private double puntajeAlerta = 0.5;
    private double puntajeCritico = 1.0;

    private List<Factor> factores = new ArrayList<>();
    private Map<Dimension, Double> pesos = new LinkedHashMap<>();
    private Niveles niveles = new Niveles();
    private Escalamiento escalamiento = new Escalamiento();
    private PerfilTermicoConfig perfilTermico = new PerfilTermicoConfig();
    private Map<String, List<Double>> plausibilidad = new LinkedHashMap<>();
    private List<Double> valoresAusentes = new ArrayList<>(List.of(-999.0));
    private List<String> parametrosInSitu = new ArrayList<>();
    private Alertas alertas = new Alertas();
    private double baf = 100000.0;

    @Getter @Setter
    public static class Factor {
        private String id;
        private String parametro;
        /** Parámetro sustituto que es cota inferior del principal (solo concluye al superar). */
        private String proxy;
        private Dimension dimension;
        private TipoFactor tipo;
        private Direccion direccion;
        private Double alerta;
        private Double alertaFria;
        private Double alertaCalida;
        private Double critico;
        private Double criticoPredador;

        public double alertaPara(PerfilTermico perfil) {
            if (perfil == PerfilTermico.FRIA && alertaFria != null) return alertaFria;
            if (perfil == PerfilTermico.CALIDA && alertaCalida != null) return alertaCalida;
            if (alerta != null) return alerta;
            if (alertaCalida != null) return alertaCalida;
            if (alertaFria != null) return alertaFria;
            throw new IllegalStateException("Factor " + id + " sin umbral de alerta");
        }

        public double criticoPara(boolean predador) {
            if (predador && criticoPredador != null) return criticoPredador;
            return critico;
        }
    }

    @Getter @Setter
    public static class Niveles {
        private double precaucion = 0.25;
        private double alerta = 0.50;
        private double critico = 0.75;
    }

    @Getter @Setter
    public static class Escalamiento {
        private String humanoCritico = "ALERTA";
        private String cualquierAlerta = "PRECAUCION";
    }

    @Getter @Setter
    public static class PerfilTermicoConfig {
        private double altitudFriaMin = 2001;
        private PerfilTermico porDefecto = PerfilTermico.CALIDA;
    }

    @Getter @Setter
    public static class Alertas {
        private String topico = "environmental-alerts";
        private List<String> nivelesPublicados = new ArrayList<>(List.of("ALERTA", "CRITICO"));
    }

    public double pesoDe(Dimension d) {
        return pesos.getOrDefault(d, 0.0);
    }
}
