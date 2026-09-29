package com.imfundokahle.model;

/**
 * Quien puede ver un material.
 * EVERYONE: aparece en el listado de alumnos, profesores y admin (por defecto).
 * TEACHERS_ONLY: queda oculto para los alumnos (uso interno / borrador del profesor).
 */
public enum MaterialVisibility {
    EVERYONE, TEACHERS_ONLY
}
