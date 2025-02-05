package uk.gov.hmcts.reform.iacasedocumentsapi.infrastructure.clients;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.UserDetailsProvider;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.UserDetails;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacasedocumentsapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacasedocumentsapi.infrastructure.enties.em.BundleCaseData;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class BundleRequestExecutorTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String ENDPOINT = "http://endpoint";
    private static final String SERVICE_TOKEN = randomAlphabetic(32);
    private static final String ACCESS_TOKEN = randomAlphabetic(32);

    @Mock private AuthTokenGenerator serviceAuthTokenGenerator;
    @Mock private RestTemplate restTemplate;

    @Mock private UserDetailsProvider userDetailsProvider;
    @Mock private UserDetails userDetails;
    @Mock private Callback<BundleCaseData> callback;
    @Mock private PreSubmitCallbackResponse<BundleCaseData> callbackResponse;
    @Mock private ResponseEntity<PreSubmitCallbackResponse<BundleCaseData>> responseEntity;


    private BundleRequestExecutor bundleRequestExecutor;

    @Before
    public void setUp() {
        bundleRequestExecutor = new BundleRequestExecutor(
                restTemplate,
                serviceAuthTokenGenerator,
                userDetailsProvider
        );

        when(serviceAuthTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);
        when(userDetailsProvider.getUserDetails()).thenReturn(userDetails);
        when(userDetails.getAccessToken()).thenReturn(ACCESS_TOKEN);
    }

    @Test
    public void should_invoke_endpoint_with_given_payload() {

        when(restTemplate
                .exchange(
                        any(String.class),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(callbackResponse);

        PreSubmitCallbackResponse<BundleCaseData> response =
                bundleRequestExecutor.post(
                        callback,
                        ENDPOINT

                );

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(callbackResponse);

        ArgumentCaptor<HttpEntity> requestEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                eq(ENDPOINT),
                eq(HttpMethod.POST),
                requestEntityCaptor.capture(),
                any(ParameterizedTypeReference.class)
        );

        HttpEntity actualRequestEntity = requestEntityCaptor.getValue();

        final String actualContentTypeHeader = actualRequestEntity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        final String actualAcceptHeader = actualRequestEntity.getHeaders().getFirst(HttpHeaders.ACCEPT);
        final String actualServiceAuthorizationHeader = actualRequestEntity.getHeaders().getFirst(SERVICE_AUTHORIZATION);
        final String actualAuthorizationHeader = actualRequestEntity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        final Callback<AsylumCase> actualPostBody = (Callback<AsylumCase>) actualRequestEntity.getBody();

        assertThat(actualContentTypeHeader).isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);
        assertThat(actualAcceptHeader).isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);
        assertThat(actualServiceAuthorizationHeader).isEqualTo(SERVICE_TOKEN);
        assertThat(actualAuthorizationHeader).isEqualTo(ACCESS_TOKEN);
        assertThat(actualPostBody).isEqualTo(callback);

    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> bundleRequestExecutor.post(null, ENDPOINT))
                .hasMessage("payload must not be null")
                .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> bundleRequestExecutor.post(callback, null))
                .hasMessage("endpoint must not be null")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void should_handle_http_server_exception_when_calling_api() {

        HttpServerErrorException underlyingException = mock(HttpServerErrorException.class);

        when(restTemplate
                .exchange(
                        eq(ENDPOINT),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)
                )).thenThrow(underlyingException);

        assertThatThrownBy(() -> bundleRequestExecutor.post(callback, ENDPOINT))
                .isExactlyInstanceOf(DocumentServiceResponseException.class)
                .hasMessageContaining("Couldn't create bundle using API")
                .hasCause(underlyingException);

    }

    @Test
    public void should_handle_http_client_exception_when_calling_api() {
        HttpClientErrorException underlyingException = mock(HttpClientErrorException.class);

        when(restTemplate
                .exchange(
                        eq(ENDPOINT),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)
                )).thenThrow(underlyingException);

        assertThatThrownBy(() -> bundleRequestExecutor.post(callback, ENDPOINT))
                .isExactlyInstanceOf(DocumentServiceResponseException.class)
                .hasMessageContaining("Couldn't create bundle using API")
                .hasCause(underlyingException);
    }


}