package com.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoice_history")
@Getter
@Setter
public class InvoiceHistory {
    @Id
    private String id;
}
