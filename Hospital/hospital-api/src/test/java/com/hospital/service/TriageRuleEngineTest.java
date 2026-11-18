package com.hospital.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriageRuleEngineTest {

    private final TriageRuleEngine engine = new TriageRuleEngine();

    @Test
    void childHighFeverByAgeBandTriggersRedFlag() {
        TriageRuleEngine.Result result = engine.assess(
                "孩子高烧39度，精神很差", null, "CHILD", "MALE", "UNKNOWN", null);

        assertTrue(result.redFlag);
        assertEquals("CHILD_HIGH_FEVER", result.redFlagPattern);
        assertEquals("RED_FLAG", result.tier);
    }

    @Test
    void childHighFeverStillWorksWithPreciseAge() {
        TriageRuleEngine.Result result = engine.assess(
                "孩子高烧39度，精神很差", 5, null, "MALE", "UNKNOWN", null);

        assertTrue(result.redFlag);
        assertEquals("CHILD_HIGH_FEVER", result.redFlagPattern);
    }

    @Test
    void elderAgeBandFallsBackToGeriatricDepartment() {
        TriageRuleEngine.Result result = engine.assess(
                "最近睡不好，吃不下饭", null, "ELDER", "MALE", "UNKNOWN", null);

        assertEquals("老年病科", result.deptName);
    }

    @Test
    void pregnancyQuestionIsNotAskedForChildBand() {
        TriageRuleEngine engine = new TriageRuleEngine();

        assertFalse(engine.needsPregnancyQuestion("孩子下腹疼", null, "CHILD", "FEMALE", "UNKNOWN"));
    }

    @Test
    void pregnancyQuestionIsAskedForAdultFemaleWithUnknownPregnancy() {
        TriageRuleEngine engine = new TriageRuleEngine();

        assertTrue(engine.needsPregnancyQuestion("下腹疼，有点出血", null, "YOUNG", "FEMALE", "UNKNOWN"));
    }
}
