package com.valiantech.core.iam.email;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.valiantech.core.iam.exception.GenericException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("prod")
public class SendGridEmailSender implements EmailSender {

    private final SendGrid sendGrid;

    @Override
    public void sendEmail(String to, String subject, String body) {
        Email from = new Email("no-reply@yourdomain.com");
        Email toEmail = new Email(to);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                throw new GenericException("SendGrid error: " + response.getBody());
            }
        } catch (Exception e) {
            throw new GenericException("Error sending email via SendGrid");
        }
    }
}
