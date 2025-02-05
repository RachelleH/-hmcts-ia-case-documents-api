package uk.gov.hmcts.reform.iacasedocumentsapi.domain.handlers.presubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.AsylumCaseDefinition.*;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.AsylumCaseDefinition;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.DocumentTag;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.DocumentWithMetadata;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.field.Document;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.field.IdValue;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.service.*;
import uk.gov.hmcts.reform.iacasedocumentsapi.infrastructure.SystemDateProvider;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class HearingBundleGeneratorTest {

    private HearingBundleGenerator hearingBundleGenerator;

    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;

    @Mock private FileNameQualifier<AsylumCase> fileNameQualifier;
    @Mock private DocumentBundler documentBundler;
    @Mock private DocumentHandler documentHandler;
    @Mock private BundleOrder bundleOrder;

    @Mock private DocumentWithMetadata documentWithMetadata;
    @Mock private Document hearingBundle;

    @Before
    public void setUp() {

        String fileExtension = "PDF";
        String fileName = "some-file-name";

        hearingBundleGenerator =
            new HearingBundleGenerator(
                fileExtension,
                fileName,
                fileNameQualifier,
                documentBundler,
                documentHandler,
                bundleOrder
            );

        when(bundleOrder.compare(any(DocumentWithMetadata.class), any(DocumentWithMetadata.class))).thenCallRealMethod();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(fileNameQualifier.get(anyString(), eq(caseDetails))).thenReturn("filename");
        when(callback.getEvent()).thenReturn(Event.GENERATE_HEARING_BUNDLE);

    }

    @Test
    public void it_can_handle_callback() {

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            for (PreSubmitCallbackStage callbackStage : PreSubmitCallbackStage.values()) {

                boolean canHandle = hearingBundleGenerator.canHandle(callbackStage, callback);

                if (event == Event.GENERATE_HEARING_BUNDLE
                    && callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT) {

                    assertTrue(canHandle);
                } else {
                    assertFalse(canHandle);
                }
            }

            reset(callback);
        }
    }

    @Test
    public void should_call_document_bundler_with_correct_params_and_attach_to_case() {

        int expectedBundleSize = 4;
        IdValue<DocumentWithMetadata> legalRepDoc = new IdValue<>("1", createDocumentWithMetadata(DocumentTag.ADDITIONAL_EVIDENCE));
        IdValue<DocumentWithMetadata> respondentDoc = new IdValue<>("1", createDocumentWithMetadata(DocumentTag.APPEAL_RESPONSE));
        IdValue<DocumentWithMetadata> hearingDoc = new IdValue<>("1", createDocumentWithMetadata(DocumentTag.HEARING_NOTICE));
        IdValue<DocumentWithMetadata> additionalEvidenceDoc = new IdValue<>("1", createDocumentWithMetadata(DocumentTag.ADDITIONAL_EVIDENCE));

        IdValue<DocumentWithMetadata> existingBundle = new IdValue<>("1", createDocumentWithMetadata(DocumentTag.HEARING_BUNDLE));

        when(asylumCase.read(HEARING_DOCUMENTS)).thenReturn(Optional.of(Lists.newArrayList(hearingDoc, existingBundle)));
        when(asylumCase.read(LEGAL_REPRESENTATIVE_DOCUMENTS)).thenReturn(Optional.of(Lists.newArrayList(legalRepDoc)));
        when(asylumCase.read(RESPONDENT_DOCUMENTS)).thenReturn(Optional.of(Lists.newArrayList(respondentDoc)));
        when(asylumCase.read(ADDITIONAL_EVIDENCE_DOCUMENTS)).thenReturn(Optional.of(Lists.newArrayList(additionalEvidenceDoc)));

        when(documentBundler.bundle(
            anyList(),
            eq("Hearing documents"),
            eq("filename")
        )).thenReturn(hearingBundle);

        PreSubmitCallbackResponse<AsylumCase> response =
            hearingBundleGenerator.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData()).isEqualTo(asylumCase);

        ArgumentCaptor<List<DocumentWithMetadata>> captor = ArgumentCaptor.forClass(List.class);

        InOrder inOrder = inOrder(documentBundler, documentHandler);
        inOrder.verify(documentBundler).bundle(captor.capture(), anyString(), anyString());
        inOrder.verify(documentHandler).addWithMetadata(any(AsylumCase.class), any(Document.class), any(AsylumCaseDefinition.class), any(DocumentTag.class));

        List<DocumentWithMetadata> value = captor.getValue();
        assertThat(value.size()).isEqualTo(expectedBundleSize);

        assertThat(value).containsOnlyOnce(
            legalRepDoc.getValue(),
            respondentDoc.getValue(),
            hearingDoc.getValue(),
            additionalEvidenceDoc.getValue()
        );
    }

    @Test
    public void should_call_document_bundler_with_when_there_are_no_documents() {

        int expectedBundleSize = 0;
        when(asylumCase.read(HEARING_DOCUMENTS)).thenReturn(Optional.empty());
        when(asylumCase.read(LEGAL_REPRESENTATIVE_DOCUMENTS)).thenReturn(Optional.empty());
        when(asylumCase.read(RESPONDENT_DOCUMENTS)).thenReturn(Optional.empty());
        when(asylumCase.read(ADDITIONAL_EVIDENCE_DOCUMENTS)).thenReturn(Optional.empty());

        when(documentBundler.bundle(
            anyList(),
            eq("Hearing documents"),
            eq("filename")
        )).thenReturn(hearingBundle);

        PreSubmitCallbackResponse<AsylumCase> response =
            hearingBundleGenerator.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback);

        assertThat(response.getData()).isEqualTo(asylumCase);

        ArgumentCaptor<List<DocumentWithMetadata>> captor = ArgumentCaptor.forClass(List.class);

        InOrder inOrder = inOrder(documentBundler, documentHandler);
        inOrder.verify(documentBundler).bundle(captor.capture(), anyString(), anyString());
        inOrder.verify(documentHandler).addWithMetadata(any(AsylumCase.class), any(Document.class), any(AsylumCaseDefinition.class), any(DocumentTag.class));

        List<DocumentWithMetadata> value = captor.getValue();
        assertThat(value.size()).isEqualTo(expectedBundleSize);

        assertThat(value).isEmpty();

    }

    @Test
    public void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> hearingBundleGenerator.handle(PreSubmitCallbackStage.ABOUT_TO_START, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);

        when(callback.getEvent()).thenReturn(Event.START_APPEAL);

        assertThatThrownBy(() -> hearingBundleGenerator.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }


    private Document createDocumentWithDescription() {
        return
            new Document("some-url",
                "some-binary-url",
                RandomStringUtils.randomAlphabetic(20));
    }

    private DocumentWithMetadata createDocumentWithMetadata(DocumentTag documentTag) {

        return
            new DocumentWithMetadata(createDocumentWithDescription(),
                RandomStringUtils.randomAlphabetic(20),
                new SystemDateProvider().now().toString(), documentTag);

    }


}
