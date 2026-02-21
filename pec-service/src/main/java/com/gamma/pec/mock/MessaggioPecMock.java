package com.gamma.pec.mock;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessaggioPecMock {
    private String id;
    private String subject;
    private String sender;
    private List<AllegatoMock> allegati;
}
