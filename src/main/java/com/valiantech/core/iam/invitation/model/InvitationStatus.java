package com.valiantech.core.iam.invitation.model;

public enum InvitationStatus {
    PENDING, // Invitacion esperando respuesta
    ACCEPTED, // Se uso, no disponibles para nuevos registros
    EXPIRED, // vencio automaticamente
    CANCELLED, // admin elimino manualmente antes de ser usada
    REJECTED // Invitado lo rechazo
}
