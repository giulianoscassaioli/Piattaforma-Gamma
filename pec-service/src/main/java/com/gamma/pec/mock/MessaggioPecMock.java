package com.gamma.pec.mock;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessaggioPecMock {
    private String id;
    private String oggetto;
    private String mittente;
    private List<AllegatoMock> allegati;
}
