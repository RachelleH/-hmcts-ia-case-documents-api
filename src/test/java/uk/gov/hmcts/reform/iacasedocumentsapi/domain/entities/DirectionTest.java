package uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class DirectionTest {

    private final String explanation = "Do the thing";
    private final Parties parties = Parties.RESPONDENT;
    private final String dateDue = "2018-12-31T12:34:56";
    private final String dateSent = "2018-12-25";
    private DirectionTag tag = DirectionTag.LEGAL_REPRESENTATIVE_REVIEW;

    private Direction direction = new Direction(
        explanation,
        parties,
        dateDue,
        dateSent,
        tag
    );

    @Test
    public void should_hold_onto_values() {

        assertEquals(explanation, direction.getExplanation());
        assertEquals(parties, direction.getParties());
        assertEquals(dateDue, direction.getDateDue());
        assertEquals(dateSent, direction.getDateSent());
        assertEquals(tag, direction.getTag());
    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> new Direction(null, parties, dateDue, dateSent, tag))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Direction(explanation, null, dateDue, dateSent, tag))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Direction(explanation, parties, null, dateSent, tag))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Direction(explanation, parties, dateDue, null, tag))
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Direction(explanation, parties, dateDue, dateSent, null))
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
