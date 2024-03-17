package com.wellsfargo.imposterhunters.imposterhunters;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpeechAnalyzeResponse {
    private String text;
    private String status;
    private double positivescore;
    private double neutralscore;
    private double negativescore;
}
