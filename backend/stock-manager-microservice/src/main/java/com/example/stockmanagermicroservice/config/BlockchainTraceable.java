package com.example.stockmanagermicroservice.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation à placer sur n'importe quelle méthode pour qu'elle soit tracée
 * dans la blockchain via le employee-microservice.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlockchainTraceable {
    String action() default "Action Non Spécifiée";
}
