-- Script para insertar roles iniciales en la base de datos

-- Roles del sistema
INSERT INTO roles (id, name, description) VALUES
(1, 'ADMINISTRADOR', 'Administrador de la plataforma de plazoleta de comidas'),
(2, 'PROPIETARIO', 'Propietario de un restaurante'),
(3, 'EMPLEADO', 'Empleado de un restaurante'),
(4, 'CLIENTE', 'Cliente de la plazoleta de comidas')
ON CONFLICT (name) DO NOTHING;

-- Crear usuario administrador inicial
INSERT INTO users (
    id, name, last_name, password, email, identification_document, phone_number, birth_date, role_id, created_at
) VALUES (
    1,
    'Admin',
    'Sistema',
    '$2a$10$p4a7VE8q0enYsvh2WMn1mO.LulXkflkDZSbHLVX.Lr8T5lW3EmfB6', -- Contraseña: Admin123
    'admin@admin.com',
    '1234567890',
    '+573001234567',
    '1990-01-01',
    1,
    NOW()
) ON CONFLICT (email) DO NOTHING;

SELECT setval('roles_id_seq', 4, true);
