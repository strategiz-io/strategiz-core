package io.strategiz.service.learn.controller;

import io.strategiz.business.aichat.AIChatBusiness;
import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.client.base.llm.model.ModelInfo;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.learn.dto.ChatRequestDto;
import io.strategiz.service.learn.dto.ChatResponseDto;
import io.strategiz.service.learn.service.LearnChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller for Learn feature AI chat
 */
@RestController
@RequestMapping("/v1/learn/chat")
@Tag(name = "Learn Chat", description = "AI-powered chat assistant for trading education")
public class LearnChatController {

	private static final Logger logger = LoggerFactory.getLogger(LearnChatController.class);

	private final LearnChatService learnChatService;

	private final AIChatBusiness aiChatBusiness;

	private final SubscriptionService subscriptionService;

	public LearnChatController(LearnChatService learnChatService, AIChatBusiness aiChatBusiness,
			SubscriptionService subscriptionService) {
		this.learnChatService = learnChatService;
		this.aiChatBusiness = aiChatBusiness;
		this.subscriptionService = subscriptionService;
	}

	@PostMapping
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Send a chat message", description = "Send a message to the AI assistant and receive a response")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successful response",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	public Mono<ResponseEntity<ChatResponseDto>> chat(@Valid @RequestBody ChatRequestDto request,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Received chat request from user: {}", userId);

		// Check if user can send message (within limits)
		if (!subscriptionService.canSendMessage(userId)) {
			logger.warn("User {} exceeded daily message limit", userId);
			return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(ChatResponseDto.error("Daily message limit exceeded. Upgrade your plan for more messages.")));
		}

		return learnChatService.chat(request, userId)
			.doOnSuccess(response -> {
				// Record usage after successful response
				if (response != null && response.getMessage() != null) {
					subscriptionService.recordMessageUsage(userId);
					logger.debug("Recorded message usage for user {}", userId);
				}
			})
			.map(response -> ResponseEntity.ok(response))
			.defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ChatResponseDto.error("Failed to generate response")));
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Send a chat message with streaming response",
			description = "Send a message and receive a streaming response (Server-Sent Events)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successful streaming response"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") })
	public Flux<ChatResponseDto> chatStream(@RequestParam String message,
			@RequestParam(required = false, defaultValue = "learn") String feature,
			@RequestParam(required = false) String currentPage,
			@RequestParam(required = false) String model,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Received streaming chat request from user: {}, model: {}", userId, model);

		// Check if user can send message (within limits)
		if (!subscriptionService.canSendMessage(userId)) {
			logger.warn("User {} exceeded daily message limit", userId);
			return Flux.just(ChatResponseDto.error("Daily message limit exceeded. Upgrade your plan for more messages."));
		}

		// Build request from query params
		ChatRequestDto request = new ChatRequestDto();
		request.setMessage(message);
		request.setFeature(feature);
		request.setCurrentPage(currentPage);
		request.setModel(model);

		// Record usage before streaming (we count the request, not completion)
		subscriptionService.recordMessageUsage(userId);

		return learnChatService.chatStream(request, userId);
	}

	@GetMapping("/health")
	@Operation(summary = "Health check", description = "Check if the chat service is available")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("Chat service is healthy");
	}

	@GetMapping("/models")
	@Operation(summary = "Get available AI models",
			description = "Returns list of available LLM models for chat (Gemini, Claude, etc.)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of available models"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	public ResponseEntity<List<ModelInfo>> getModels() {
		logger.debug("Getting available AI models");
		return ResponseEntity.ok(aiChatBusiness.getAvailableModels());
	}

}
