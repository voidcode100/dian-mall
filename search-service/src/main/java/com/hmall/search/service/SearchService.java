package com.hmall.search.service;

import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.dto.CategoryAndBrandDTO;
import com.hmall.search.domain.dto.ItemDTO;
import com.hmall.search.domain.query.ItemPageQuery;

public interface SearchService {
    PageDTO<ItemDTO> search(ItemPageQuery query);

    CategoryAndBrandDTO filter(ItemPageQuery query);
}
