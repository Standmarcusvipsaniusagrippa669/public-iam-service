package com.valiantech.core.iam.email;

import org.springframework.scheduling.annotation.Async;

public interface EmailSender {

    /**
     * Envía un email con los parámetros indicados.
     *
     * @param to      destinatario
     * @param subject asunto del correo
     * @param body    contenido HTML o texto plano del correo
     */
    @Async
    void sendEmail(String to, String subject, String body);
}
