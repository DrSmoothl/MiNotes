package net.micode.notes.domain.repository;

public interface IntroductionRepository {
    boolean isIntroductionCreated();

    String loadIntroductionText();

    void markIntroductionCreated();
}