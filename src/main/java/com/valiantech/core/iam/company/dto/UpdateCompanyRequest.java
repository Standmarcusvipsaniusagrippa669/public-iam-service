package com.valiantech.core.iam.company.dto;

import com.valiantech.core.iam.company.model.CompanyStatus;

/**
 * Request para la actualización de los datos de una empresa (compañía).
 *
 * <p>
 * Este DTO es utilizado para modificar los atributos de una empresa existente. Todos los campos son opcionales,
 * por lo que solo se actualizarán aquellos que no sean nulos en el request. No permite cambiar el RUT.
 * </p>
 *
 * <b>Campos (opcionales):</b>
 * <ul>
 *   <li><b>businessName:</b> Nueva razón social registrada.</li>
 *   <li><b>tradeName:</b> Nuevo nombre de fantasía o comercial.</li>
 *   <li><b>activity:</b> Nuevo giro o actividad económica.</li>
 *   <li><b>address:</b> Nueva dirección principal.</li>
 *   <li><b>commune:</b> Nueva comuna.</li>
 *   <li><b>region:</b> Nueva región administrativa.</li>
 *   <li><b>email:</b> Nuevo correo de contacto principal.</li>
 *   <li><b>phone:</b> Nuevo teléfono de contacto.</li>
 *   <li><b>logoUrl:</b> Nueva URL del logo de la empresa.</li>
 *   <li><b>status:</b> Nuevo estado de la empresa ({@link CompanyStatus}).</li>
 * </ul>
 *
 * <h3>Reglas de negocio:</h3>
 * <ul>
 *   <li>Sólo los campos no nulos serán aplicados a la entidad.</li>
 *   <li>No se puede modificar el RUT de la empresa mediante este request.</li>
 *   <li>El campo {@code status} puede ser utilizado para suspender, activar o archivar la empresa según reglas del negocio.</li>
 * </ul>
 *
 * @param businessName Nueva razón social registrada.
 * @param tradeName    Nuevo nombre de fantasía o comercial.
 * @param activity     Nuevo giro o actividad económica.
 * @param address      Nueva dirección principal.
 * @param commune      Nueva comuna.
 * @param region       Nueva región administrativa.
 * @param email        Nuevo correo de contacto principal.
 * @param phone        Nuevo teléfono de contacto.
 * @param logoUrl      Nueva URL del logo de la empresa.
 * @param status       Nuevo estado de la empresa.
 */
public record UpdateCompanyRequest(
        String businessName,
        String tradeName,
        String activity,
        String address,
        String commune,
        String region,
        String email,
        String phone,
        String logoUrl,
        CompanyStatus status
) {}

