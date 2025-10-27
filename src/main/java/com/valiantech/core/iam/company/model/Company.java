package com.valiantech.core.iam.company.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa una empresa registrada en el sistema IAM.
 *
 * <p>
 * Modela los atributos principales de una compañía, incluyendo sus datos administrativos,
 * estado y metadatos de auditoría (creación y actualización).
 * Se utiliza para el almacenamiento persistente en la tabla {@code companies}.
 * </p>
 *
 * <b>Mapeo de campos:</b>
 * <ul>
 *   <li><b>id:</b> Identificador único de la empresa (UUID, PK).</li>
 *   <li><b>rut:</b> RUT chileno (máx. 10 caracteres).</li>
 *   <li><b>businessName:</b> Razón social registrada en el SII.</li>
 *   <li><b>tradeName:</b> Nombre de fantasía o comercial.</li>
 *   <li><b>activity:</b> Giro o actividad económica.</li>
 *   <li><b>address:</b> Dirección principal.</li>
 *   <li><b>commune:</b> Comuna.</li>
 *   <li><b>region:</b> Región administrativa.</li>
 *   <li><b>email:</b> Correo de contacto principal.</li>
 *   <li><b>phone:</b> Teléfono de contacto.</li>
 *   <li><b>logoUrl:</b> URL del logo (campo texto, puede ser nulo o largo).</li>
 *   <li><b>status:</b> Estado de la empresa ({@link CompanyStatus}).</li>
 *   <li><b>createdAt:</b> Fecha de creación del registro.</li>
 *   <li><b>updatedAt:</b> Fecha de última modificación.</li>
 * </ul>
 *
 * <b>Notas:</b>
 * <ul>
 *   <li>El campo {@code rut} debería estar indexado y/o tener restricción de unicidad en base de datos.</li>
 *   <li>La entidad no incluye relaciones directas para reducir acoplamiento y facilitar migración a microservicios.</li>
 *   <li>El campo {@code status} permite distinguir empresas activas, suspendidas, archivadas, etc.</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    private UUID id;

    @Column(length = 10)
    private String rut;

    private String businessName;

    private String tradeName;

    private String activity;

    private String address;

    private String commune;

    private String region;

    private String email;

    private String phone;

    @Column(columnDefinition = "text")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    private CompanyStatus status;

    private Instant createdAt;

    private Instant updatedAt;
}
