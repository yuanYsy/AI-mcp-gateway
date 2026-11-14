package com.yuan.aimcpgateway;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;
    @Autowired
    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    @PostMapping("/completions")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        // 1. 组装 Prompt
        String prompt = request.getMessages().stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 2. 调用 AI 模型
        org.springframework.ai.chat.model.ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        // 3. 转换响应格式为 OpenAI 兼容格式
        return convertToOpenAIFormat(response, request.getModel());
    }

    private ChatResponse convertToOpenAIFormat(org.springframework.ai.chat.model.ChatResponse response, String model) {
        ChatResponse result = new ChatResponse();
        result.setId("chat-" + UUID.randomUUID().toString().substring(0, 8));
        result.setModel(model != null ? model : "qwen-turbo");

        ChatResponse.Choice choice = new ChatResponse.Choice();
        ChatResponse.ChatMessage msg = new ChatResponse.ChatMessage();
        msg.setRole("assistant");
        msg.setContent(response.getResult().getOutput().getText());
        choice.setMessage(msg);
        choice.setFinishReason("stop");
        result.setChoices(List.of(choice));

        // 模拟 Usage 数据（后续接入真实 Token 统计）
        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(5);
        usage.setTotalTokens(15);
        result.setUsage(usage);

        return result;
    }
}