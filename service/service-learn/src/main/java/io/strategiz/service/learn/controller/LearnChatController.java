package io.strategiz.service.learn.controller;

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

/**
 * REST controller for Learn feature AI chat
 */
@RestController
@RequestMapping("/v1/learn/chat")
@Tag(name = "Learn Chat", description = "AI-powered chat assistant for trading education")
public class LearnChatController {

	private static final Logger logger = LoggerFactory.getLogger(LearnChatController.class);

	private final LearnChatService learnChatService;

	public LearnChatController(LearnChatService learnChatService) {
		this.learnChatService = learnChatService;
	}

	@PostMapping
	@Operation(summary = "Send a chat message", description = "Send a message to the AI assistant and receive a response")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successful response",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "Internal server error") })
	public Mono<ResponseEntity<ChatResponseDto>> chat(@Valid @RequestBody ChatRequestDto request,
			@RequestHeader(value = "X-User-Id", required = false) String userId) {

		logger.info("Received chat request from user: {}", userId);

		// TODO: Extract userId from authentication token instead of header
		String authenticatedUserId = userId != null ? userId : "anonymous";

		return learnChatService.chat(request, authenticatedUserId)
			.map(response -> ResponseEntity.ok(response))
			.defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ChatResponseDto.error("Failed to generate response")));
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Send a chat message with streaming response",
			description = "Send a message and receive a streaming response (Server-Sent Events)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successful streaming response"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") })
	public Flux<ChatResponseDto> chatStream(@RequestParam String message,
			@RequestParam(required = false, defaultValue = "learn") String feature,
			@RequestParam(required = false) String currentPage,
			@RequestHeader(value = "X-User-Id", required = false) String userId) {

		logger.info("Received streaming chat request from user: {}", userId);

		// Build request from query params
		ChatRequestDto request = new ChatRequestDto();
		request.setMessage(message);
		request.setFeature(feature);
		request.setCurrentPage(currentPage);

		String authenticatedUserId = userId != null ? userId : "anonymous";

		return learnChatService.chatStream(request, authenticatedUserId);
	}

	@GetMapping("/health")
	@Operation(summary = "Health check", description = "Check if the chat service is available")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("Chat service is healthy");
	}

}
