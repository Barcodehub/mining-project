package com.mining.soft.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "ambiental")
@Getter @Setter
public class AmbientalConfig {

    private String region;
    private String version;

    private Threshold hgDisuelto = new Threshold();
    private Threshold hgTotal = new Threshold();
    private Threshold metilmercurio = new Threshold();
    private SedimentoThreshold hgSedimento = new SedimentoThreshold();
    private Threshold hgPeces = new Threshold();
    private PhThreshold ph = new PhThreshold();
    private RedoxThreshold redox = new RedoxThreshold();
    private Threshold oxigenoDisuelto = new Threshold();
    private double carbonoOrganico = 5.0;
    private Threshold turbidez = new Threshold();
    private Threshold temperatura = new Threshold();
    private PesosScore pesosScore = new PesosScore();
    private double baf = 100000.0;

    @Getter @Setter
    public static class Threshold {
        private double alerta;
        private double critico;
    }

    @Getter @Setter
    public static class SedimentoThreshold {
        private double tel;
        private double pel;
    }

    @Getter @Setter
    public static class PhThreshold {
        private double minimo;
        private double optimoMin;
        private double optimoMax;
        private double criticoBajo;
    }

    @Getter @Setter
    public static class RedoxThreshold {
        private double criticoBajo;
        private double alertaBajo;
    }

    @Getter @Setter
    public static class PesosScore {
        private double riesgoHumano = 0.45;
        private double riesgoEcologico = 0.35;
        private double riesgoGeoquimico = 0.20;
    }
}
