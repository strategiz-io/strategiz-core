package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.CreateProviderRequest;
import io.strategiz.service.provider.model.response.CreateProviderResponse;
import io.strategiz.service.provider.service.CreateProviderService;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controller for creating provider connections and integrations. Handles OAuth initiation
 * and API key setup for various providers.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@RestController("providerCreateProviderController")
@RequestMapping("/v1/providers")
@RequireAuth(minAcr = "1")
public class CreateProviderController extends BaseController {

	@Override
	protected String getModuleName() {
		return "service-provider";
	}

	private final CreateProviderService createProviderService;

	@Autowired
	public CreateProviderController(CreateProviderService createProviderService) {
		this.createProviderService = createProviderService;
	}

	/**
	 * Creates a new provider connection.
	 *
	 * For OAuth providers: Returns authorization URL and state For API key providers:
	 * Validates credentials and creates connection
	 * @param request The provider connection request
	 * @param user The authenticated user from HTTP-only cookie
	 * @return CreateProviderResponse containing connection details or OAuth URL
	 */
	@PostMapping
	public ResponseEntity<CreateProviderResponse> createProvider(@Valid @RequestBody CreateProviderRequest request,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		log.info("CreateProvider: userId: {}", userId);

		request.setUserId(userId);

		log.info("Creating provider connection for user: {}, provider: {}, type: {}", userId, request.getProviderId(),
				request.getConnectionType());

		// Debug log to check credentials
		if (request.getCredentials() != null) {
			log.debug("Received credentials map with {} keys: {}", request.getCredentials().size(),
					request.getCredentials().keySet());
		}
		else {
			log.debug("No credentials provided in request");
		}

		// Validate required fields
		if (request.getProviderId() == null || request.getProviderId().isEmpty()) {
			throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider",
					"providerId");
		}

		if (request.getConnectionType() == null || request.getConnectionType().isEmpty()) {
			throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider",
					"connectionType");
		}

		CreateProviderResponse response = createProviderService.createProvider(request);

		log.info("Provider connection created successfully for user: {}, provider: {}, status: {}", userId,
				request.getProviderId(), response.getStatus());

		// Return 201 Created for successful provider creation
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Fetches connected providers for the authenticated user.
	 * @param user The authenticated user from HTTP-only cookie
	 * @return Response containing the list of connected providers
	 */
	@GetMapping("/connected")
	public ResponseEntity<?> getConnectedProviders(@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		log.info("Fetching connected providers for user: {}", userId);

		try {
			var connectedProviders = createProviderService.getConnectedProviders(userId);
			return ResponseEntity.ok(connectedProviders);
		}
		catch (Exception e) {
			log.error("Error fetching connected providers for user {}: {}", userId, e.getMessage(), e);
			throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_CONNECTION_FAILED, "service-provider",
					"Failed to fetch connected providers");
		}
	}

	/**
	 * Health check endpoint for the create provider service.
	 * @return Simple health status
	 */
	@GetMapping("/create/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("CreateProviderController is healthy");
	}

}