package io.strategiz.service.console.controller;

import io.strategiz.data.preferences.entity.AiModelConfig;
import io.strategiz.data.preferences.entity.PlatformConfig;
import io.strategiz.data.preferences.entity.StratPackConfig;
import io.strategiz.data.preferences.entity.TierConfig;
import io.strategiz.data.preferences.repository.AiModelConfigRepository;
import io.strategiz.data.preferences.repository.PlatformConfigRepository;
import io.strategiz.data.preferences.repository.StratPackConfigRepository;
import io.strategiz.data.preferences.repository.TierConfigRepository;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing platform configuration.
 *
 * <p>
 * This controller provides admin APIs for configuring:
 * </p>
 * <ul>
 * <li>Platform Config - Global platform settings (fees, rates)</li>
 * <li>Tier Config - Subscription tier limits and pricing</li>
 * <li>STRAT Pack Config - STRAT token pack definitions</li>
 * <li>AI Model Config - AI model costs and tier requirements</li>
 * </ul>
 *
 * <p>
 * All changes take effect immediately (with caching).
 * </p>
 */
@RestController
@RequestMapping("/v1/console/config")
@Tag(name = "Admin - Platform Config", description = "Platform configuration management for administrators")
public class AdminConfigController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final PlatformConfigRepository platformConfigRepository;

	private final TierConfigRepository tierConfigRepository;

	private final StratPackConfigRepository stratPackConfigRepository;

	private final AiModelConfigRepository aiModelConfigRepository;

	@Autowired
	public AdminConfigController(@Autowired(required = false) PlatformConfigRepository platformConfigRepository,
			@Autowired(required = false) TierConfigRepository tierConfigRepository,
			@Autowired(required = false) StratPackConfigRepository stratPackConfigRepository,
			@Autowired(required = false) AiModelConfigRepository aiModelConfigRepository) {
		this.platformConfigRepository = platformConfigRepository;
		this.tierConfigRepository = tierConfigRepository;
		this.stratPackConfigRepository = stratPackConfigRepository;
		this.aiModelConfigRepository = aiModelConfigRepository;
		log.info("AdminConfigController initialized");
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	private ResponseEntity<Map<String, Object>> serviceUnavailable(String service) {
		Map<String, Object> error = new HashMap<>();
		error.put("error", service + " service not available");
		error.put("status", 503);
		return ResponseEntity.status(503).body(error);
	}

	// ==================== Platform Config ====================

	@GetMapping("/platform")
	@Operation(summary = "Get platform config", description = "Returns the current platform configuration")
	public ResponseEntity<?> getPlatformConfig(HttpServletRequest request) {
		if (platformConfigRepository == null) {
			return serviceUnavailable("PlatformConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getPlatformConfig", adminUserId);

		PlatformConfig config = platformConfigRepository.getCurrent();
		return ResponseEntity.ok(config);
	}

	@PutMapping("/platform")
	@Operation(summary = "Update platform config", description = "Updates the platform configuration")
	public ResponseEntity<?> updatePlatformConfig(@RequestBody PlatformConfig config, HttpServletRequest request) {
		if (platformConfigRepository == null) {
			return serviceUnavailable("PlatformConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("updatePlatformConfig", adminUserId);

		PlatformConfig saved = platformConfigRepository.saveConfig(config, adminUserId);
		log.info("Platform config updated by admin {}", adminUserId);

		return ResponseEntity.ok(saved);
	}

	// ==================== Tier Config ====================

	@GetMapping("/tiers")
	@Operation(summary = "Get all tier configs", description = "Returns all subscription tier configurations")
	public ResponseEntity<?> getAllTierConfigs(HttpServletRequest request) {
		if (tierConfigRepository == null) {
			return serviceUnavailable("TierConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getAllTierConfigs", adminUserId);

		List<TierConfig> tiers = tierConfigRepository.getAllTiers();

		Map<String, Object> response = new HashMap<>();
		response.put("tiers", tiers);
		response.put("count", tiers.size());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/tiers/{tierId}")
	@Operation(summary = "Get tier config", description = "Returns a specific tier configuration")
	public ResponseEntity<?> getTierConfig(@PathVariable String tierId, HttpServletRequest request) {
		if (tierConfigRepository == null) {
			return serviceUnavailable("TierConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getTierConfig", adminUserId, Map.of("tierId", tierId));

		TierConfig config = tierConfigRepository.getByTierId(tierId);
		return ResponseEntity.ok(config);
	}

	@PutMapping("/tiers/{tierId}")
	@Operation(summary = "Update tier config", description = "Updates a tier configuration")
	public ResponseEntity<?> updateTierConfig(@PathVariable String tierId, @RequestBody TierConfig config,
			HttpServletRequest request) {
		if (tierConfigRepository == null) {
			return serviceUnavailable("TierConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("updateTierConfig", adminUserId, Map.of("tierId", tierId));

		config.setTierId(tierId);
		TierConfig saved = tierConfigRepository.saveConfig(config, adminUserId);
		log.info("Tier config {} updated by admin {}", tierId, adminUserId);

		return ResponseEntity.ok(saved);
	}

	@PostMapping("/tiers/refresh-cache")
	@Operation(summary = "Refresh tier cache", description = "Clears the tier config cache")
	public ResponseEntity<Map<String, Object>> refreshTierCache(HttpServletRequest request) {
		if (tierConfigRepository == null) {
			return serviceUnavailable("TierConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("refreshTierCache", adminUserId);

		tierConfigRepository.clearCache();
		log.info("Tier config cache cleared by admin {}", adminUserId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Tier config cache cleared");

		return ResponseEntity.ok(response);
	}

	// ==================== STRAT Pack Config ====================

	@GetMapping("/strat-packs")
	@Operation(summary = "Get all STRAT packs", description = "Returns all STRAT pack configurations")
	public ResponseEntity<?> getAllStratPacks(HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getAllStratPacks", adminUserId);

		List<StratPackConfig> packs = stratPackConfigRepository.getAllPacks();

		Map<String, Object> response = new HashMap<>();
		response.put("packs", packs);
		response.put("count", packs.size());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/strat-packs/enabled")
	@Operation(summary = "Get enabled STRAT packs", description = "Returns only enabled STRAT packs (for purchase)")
	public ResponseEntity<?> getEnabledStratPacks(HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getEnabledStratPacks", adminUserId);

		List<StratPackConfig> packs = stratPackConfigRepository.getEnabledPacks();

		Map<String, Object> response = new HashMap<>();
		response.put("packs", packs);
		response.put("count", packs.size());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/strat-packs/{packId}")
	@Operation(summary = "Get STRAT pack", description = "Returns a specific STRAT pack configuration")
	public ResponseEntity<?> getStratPack(@PathVariable String packId, HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getStratPack", adminUserId, Map.of("packId", packId));

		return stratPackConfigRepository.getByPackId(packId)
			.map(pack -> ResponseEntity.ok((Object) pack))
			.orElse(ResponseEntity.notFound().build());
	}

	@PutMapping("/strat-packs/{packId}")
	@Operation(summary = "Update STRAT pack", description = "Updates a STRAT pack configuration")
	public ResponseEntity<?> updateStratPack(@PathVariable String packId, @RequestBody StratPackConfig config,
			HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("updateStratPack", adminUserId, Map.of("packId", packId));

		config.setPackId(packId);
		StratPackConfig saved = stratPackConfigRepository.saveConfig(config, adminUserId);
		log.info("STRAT pack {} updated by admin {}", packId, adminUserId);

		return ResponseEntity.ok(saved);
	}

	@PostMapping("/strat-packs")
	@Operation(summary = "Create STRAT pack", description = "Creates a new STRAT pack")
	public ResponseEntity<?> createStratPack(@RequestBody StratPackConfig config, HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("createStratPack", adminUserId, Map.of("packId", config.getPackId()));

		StratPackConfig saved = stratPackConfigRepository.saveConfig(config, adminUserId);
		log.info("STRAT pack {} created by admin {}", config.getPackId(), adminUserId);

		return ResponseEntity.ok(saved);
	}

	@PostMapping("/strat-packs/{packId}/enable")
	@Operation(summary = "Enable STRAT pack", description = "Enables a STRAT pack for purchase")
	public ResponseEntity<?> enableStratPack(@PathVariable String packId, HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("enableStratPack", adminUserId, Map.of("packId", packId));

		return stratPackConfigRepository.getByPackId(packId).map(pack -> {
			pack.setEnabled(true);
			StratPackConfig saved = stratPackConfigRepository.saveConfig(pack, adminUserId);
			log.info("STRAT pack {} enabled by admin {}", packId, adminUserId);
			return ResponseEntity.ok((Object) saved);
		}).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/strat-packs/{packId}/disable")
	@Operation(summary = "Disable STRAT pack", description = "Disables a STRAT pack from purchase")
	public ResponseEntity<?> disableStratPack(@PathVariable String packId, HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("disableStratPack", adminUserId, Map.of("packId", packId));

		return stratPackConfigRepository.getByPackId(packId).map(pack -> {
			pack.setEnabled(false);
			StratPackConfig saved = stratPackConfigRepository.saveConfig(pack, adminUserId);
			log.info("STRAT pack {} disabled by admin {}", packId, adminUserId);
			return ResponseEntity.ok((Object) saved);
		}).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/strat-packs/seed-defaults")
	@Operation(summary = "Seed default STRAT packs", description = "Seeds default STRAT packs if none exist")
	public ResponseEntity<Map<String, Object>> seedDefaultStratPacks(HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("seedDefaultStratPacks", adminUserId);

		stratPackConfigRepository.seedDefaultsIfEmpty(adminUserId);
		log.info("Default STRAT packs seeded by admin {}", adminUserId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Default STRAT packs seeded (if none existed)");

		return ResponseEntity.ok(response);
	}

	@PostMapping("/strat-packs/refresh-cache")
	@Operation(summary = "Refresh STRAT pack cache", description = "Clears the STRAT pack config cache")
	public ResponseEntity<Map<String, Object>> refreshStratPackCache(HttpServletRequest request) {
		if (stratPackConfigRepository == null) {
			return serviceUnavailable("StratPackConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("refreshStratPackCache", adminUserId);

		stratPackConfigRepository.clearCache();
		log.info("STRAT pack config cache cleared by admin {}", adminUserId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "STRAT pack config cache cleared");

		return ResponseEntity.ok(response);
	}

	// ==================== AI Model Config ====================

	@GetMapping("/ai-models")
	@Operation(summary = "Get all AI models", description = "Returns all AI model configurations")
	public ResponseEntity<?> getAllAiModels(HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getAllAiModels", adminUserId);

		List<AiModelConfig> models = aiModelConfigRepository.getAllModels();

		Map<String, Object> response = new HashMap<>();
		response.put("models", models);
		response.put("count", models.size());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/ai-models/enabled")
	@Operation(summary = "Get enabled AI models", description = "Returns only enabled AI models")
	public ResponseEntity<?> getEnabledAiModels(HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getEnabledAiModels", adminUserId);

		List<AiModelConfig> models = aiModelConfigRepository.getEnabledModels();

		Map<String, Object> response = new HashMap<>();
		response.put("models", models);
		response.put("count", models.size());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/ai-models/tier/{tierLevel}")
	@Operation(summary = "Get AI models for tier", description = "Returns AI models available to a specific tier level")
	public ResponseEntity<?> getAiModelsForTier(@PathVariable int tierLevel, HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getAiModelsForTier", adminUserId, Map.of("tierLevel", tierLevel));

		List<AiModelConfig> models = aiModelConfigRepository.getModelsForTier(tierLevel);

		Map<String, Object> response = new HashMap<>();
		response.put("models", models);
		response.put("tierLevel", tierLevel);
		response.put("count", models.size());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/ai-models/provider/{provider}")
	@Operation(summary = "Get AI models by provider", description = "Returns AI models from a specific provider")
	public ResponseEntity<?> getAiModelsByProvider(@PathVariable String provider, HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getAiModelsByProvider", adminUserId, Map.of("provider", provider));

		List<AiModelConfig> models = aiModelConfigRepository.getByProvider(provider);

		Map<String, Object> response = new HashMap<>();
		response.put("models", models);
		response.put("provider", provider);
		response.put("count", models.size());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/ai-models/{modelId}")
	@Operation(summary = "Get AI model", description = "Returns a specific AI model configuration")
	public ResponseEntity<?> getAiModel(@PathVariable String modelId, HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getAiModel", adminUserId, Map.of("modelId", modelId));

		return aiModelConfigRepository.getByModelId(modelId)
			.map(model -> ResponseEntity.ok((Object) model))
			.orElse(ResponseEntity.notFound().build());
	}

	@PutMapping("/ai-models/{modelId}")
	@Operation(summary = "Update AI model", description = "Updates an AI model configuration")
	public ResponseEntity<?> updateAiModel(@PathVariable String modelId, @RequestBody AiModelConfig config,
			HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("updateAiModel", adminUserId, Map.of("modelId", modelId));

		config.setModelId(modelId);
		AiModelConfig saved = aiModelConfigRepository.saveConfig(config, adminUserId);
		log.info("AI model {} updated by admin {}", modelId, adminUserId);

		return ResponseEntity.ok(saved);
	}

	@PostMapping("/ai-models")
	@Operation(summary = "Create AI model", description = "Creates a new AI model configuration")
	public ResponseEntity<?> createAiModel(@RequestBody AiModelConfig config, HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("createAiModel", adminUserId, Map.of("modelId", config.getModelId()));

		AiModelConfig saved = aiModelConfigRepository.saveConfig(config, adminUserId);
		log.info("AI model {} created by admin {}", config.getModelId(), adminUserId);

		return ResponseEntity.ok(saved);
	}

	@PostMapping("/ai-models/{modelId}/enable")
	@Operation(summary = "Enable AI model", description = "Enables an AI model")
	public ResponseEntity<?> enableAiModel(@PathVariable String modelId, HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("enableAiModel", adminUserId, Map.of("modelId", modelId));

		return aiModelConfigRepository.getByModelId(modelId).map(model -> {
			model.setEnabled(true);
			AiModelConfig saved = aiModelConfigRepository.saveConfig(model, adminUserId);
			log.info("AI model {} enabled by admin {}", modelId, adminUserId);
			return ResponseEntity.ok((Object) saved);
		}).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/ai-models/{modelId}/disable")
	@Operation(summary = "Disable AI model", description = "Disables an AI model")
	public ResponseEntity<?> disableAiModel(@PathVariable String modelId, HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("disableAiModel", adminUserId, Map.of("modelId", modelId));

		return aiModelConfigRepository.getByModelId(modelId).map(model -> {
			model.setEnabled(false);
			AiModelConfig saved = aiModelConfigRepository.saveConfig(model, adminUserId);
			log.info("AI model {} disabled by admin {}", modelId, adminUserId);
			return ResponseEntity.ok((Object) saved);
		}).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/ai-models/seed-defaults")
	@Operation(summary = "Seed default AI models", description = "Seeds default AI models if none exist")
	public ResponseEntity<Map<String, Object>> seedDefaultAiModels(HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("seedDefaultAiModels", adminUserId);

		aiModelConfigRepository.seedDefaultsIfEmpty(adminUserId);
		log.info("Default AI models seeded by admin {}", adminUserId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Default AI models seeded (if none existed)");

		return ResponseEntity.ok(response);
	}

	@PostMapping("/ai-models/refresh-cache")
	@Operation(summary = "Refresh AI model cache", description = "Clears the AI model config cache")
	public ResponseEntity<Map<String, Object>> refreshAiModelCache(HttpServletRequest request) {
		if (aiModelConfigRepository == null) {
			return serviceUnavailable("AiModelConfig");
		}
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("refreshAiModelCache", adminUserId);

		aiModelConfigRepository.clearCache();
		log.info("AI model config cache cleared by admin {}", adminUserId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "AI model config cache cleared");

		return ResponseEntity.ok(response);
	}

	// ==================== Bulk Operations ====================

	@PostMapping("/refresh-all-caches")
	@Operation(summary = "Refresh all caches", description = "Clears all configuration caches")
	public ResponseEntity<Map<String, Object>> refreshAllCaches(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("refreshAllCaches", adminUserId);

		if (tierConfigRepository != null) {
			tierConfigRepository.clearCache();
		}
		if (stratPackConfigRepository != null) {
			stratPackConfigRepository.clearCache();
		}
		if (aiModelConfigRepository != null) {
			aiModelConfigRepository.clearCache();
		}

		log.info("All config caches cleared by admin {}", adminUserId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "All configuration caches cleared");

		return ResponseEntity.ok(response);
	}

}
