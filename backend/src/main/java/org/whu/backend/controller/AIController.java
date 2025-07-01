package org.whu.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.common.Result;
import org.whu.backend.service.AIService;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/ask")
    public Result<String> askAI(@RequestBody String question) {
        String answer = aiService.getSimpleCompletion(question);
        return Result.success(answer);
    }

    /**
     * [新增] 流式问答接口
     * produces = MediaType.TEXT_EVENT_STREAM_VALUE 告诉浏览器这是一个SSE (Server-Sent Events) 流
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askAIStream(@RequestBody String question) {
        return aiService.getStreamingCompletion(question);
    }
}