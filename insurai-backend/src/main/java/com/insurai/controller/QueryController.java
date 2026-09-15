package com.insurai.controller;

import com.insurai.service.QueryService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public Map<String, Object> processQuery(@RequestBody Map<String, String> payload) {
        String query = payload.get("query");
        return queryService.process(query);
    }
}
