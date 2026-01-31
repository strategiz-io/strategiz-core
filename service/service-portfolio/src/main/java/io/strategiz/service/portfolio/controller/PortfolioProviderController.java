package io.strategiz.service.portfolio.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.ProviderPortfolioResponse;
import io.strategiz.service.portfolio.service.PortfolioProviderService;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for provider-specific portfolio operations. Single Responsibility:
 * Handles individual provider portfolio endpoints.
 */
@RestController
@RequestMapping(ServicePortfolioConstants.BASE_PATH + ServicePortfolioConstants.PROVIDERS_PATH)
@CrossOrigin(origins = "${strategiz.cors.allowed-origins:*}")
@RequireAuth(minAcr = "1")
public class PortfolioProviderController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(PortfolioProviderController.class);

	private final PortfolioProviderService portfolioProviderService;

	@Autowired
	public PortfolioProviderController(PortfolioProviderService portfolioProviderService) {
		this.portfolioProviderService = portfolioProviderService;
	}

	@Override
	protected String getModuleName() {
		return ServicePortfolioConstants.MODULE_NAME;
	}

	/**
	 * Get portfolio data for a specific provider.
	 * @param providerId Provider ID (kraken, coinbase, etc.)
	 * @param user The authenticated user from HTTP-only cookie
	 * @return Provider portfolio data
	 */
	@GetMapping("/{providerId}")
	public ResponseEntity<ProviderPortfolioResponse> getProviderPortfolio(@PathVariable String providerId,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		log.info("Fetching {} portfolio for user: {}", providerId, userId);

		try {
			ProviderPortfolioResponse portfolioData = portfolioProviderService.getProviderPortfolio(userId, providerId);

			if (portfolioData == null) {
				log.warn("No data found for provider {} and user {}", providerId, userId);
				throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
						ServicePortfolioConstants.ERROR_PROVIDER_NOT_FOUND);
			}

			return ResponseEntity.ok(portfolioData);

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error fetching {} portfolio for user {}: {}", providerId, userId, e.getMessage(), e);
			throw new StrategizException(ErrorCode.INTERNAL_ERROR,
					ServicePortfolioConstants.ERROR_PORTFOLIO_FETCH_FAILED);
		}
	}

}