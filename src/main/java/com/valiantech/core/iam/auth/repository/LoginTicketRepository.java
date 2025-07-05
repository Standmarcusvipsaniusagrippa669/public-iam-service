package com.valiantech.core.iam.auth.repository;

import com.valiantech.core.iam.auth.model.LoginTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginTicketRepository extends JpaRepository<LoginTicket, String> {
}
