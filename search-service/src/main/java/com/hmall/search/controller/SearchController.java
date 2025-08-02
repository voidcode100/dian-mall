package com.hmall.search.controller;


import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.dto.CategoryAndBrandDTO;
import com.hmall.search.domain.dto.ItemDTO;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.service.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;
    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        return searchService.search(query);
    }

    @ApiOperation("动态过滤器")
    @PostMapping("/filters")
    public CategoryAndBrandDTO filter(@RequestBody ItemPageQuery query){
        return searchService.filter(query);
    }
}
