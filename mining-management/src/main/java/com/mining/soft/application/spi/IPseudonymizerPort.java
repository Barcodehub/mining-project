package com.mining.soft.application.spi;

public interface IPseudonymizerPort {
    /** Seudónimo estable; sin la clave secreta no permite recuperar el id original. */
    String pseudonym(Long userId);
}
