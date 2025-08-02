package com.hmall.search.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.dto.CategoryAndBrandDTO;
import com.hmall.search.domain.dto.ItemDTO;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.service.SearchService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceimpl implements SearchService {
    @Override
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        PageDTO<ItemDTO> result = new PageDTO<>();
        if(query == null){
            return result;
        }
        //连接客户端
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.249.130:9200")
        ));
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        //字段文本匹配查询
        String name = query.getKey();
        if(StrUtil.isNotBlank(name)){
            builder.must(QueryBuilders.matchQuery("name",name));
        }
        //品牌分类过滤
        String brand = query.getBrand();
        if(StrUtil.isNotBlank(brand)){
            builder.filter(QueryBuilders.termQuery("brand",brand));
        }

        //商品分类过滤
        String category = query.getCategory();
        if(StrUtil.isNotBlank(category)){
            builder.filter(QueryBuilders.termQuery("category",category));
        }

        //价格范围过滤
        Integer minPrice = query.getMinPrice();
        Integer maxPrice = query.getMaxPrice();
        if(minPrice!=null&&maxPrice!=null){
            builder.filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        }

        //分页查询
        int pageNo = query.getPageNo();
        int pageSize = query.getPageSize();
        int indexFrom = (pageNo-1)*pageSize;
        request.source()
                .query(QueryBuilders.functionScoreQuery(
                        //bool构造
                        builder,
                        //function score（根据广告推荐）
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        QueryBuilders.termQuery("isAD",true),
                                        ScoreFunctionBuilders.weightFactorFunction(100)
                                )
                        }
                ).boostMode(CombineFunction.MULTIPLY))
                .from(indexFrom)
                .size(pageSize)
                .sort("updateTime",query.getIsAsc()? SortOrder.ASC : SortOrder.DESC)
                .highlighter(SearchSourceBuilder.highlight().field("name"));

        //解析数据
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            SearchHit[] hits = searchHits.getHits();
            List<ItemDTO> itemDTOS = new ArrayList<>();
            long value = searchHits.getTotalHits().value;
            result.setTotal(value);
            result.setPages(value%pageSize==0? value/pageSize : value/pageSize+1);

            for (SearchHit hit : hits) {
                String json = hit.getSourceAsString();

                ItemDTO itemDTO = JSONUtil.toBean(json, ItemDTO.class);
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if(highlightFields!=null&&!highlightFields.isEmpty()){
                    HighlightField highlightName = highlightFields.get("name");
                    name = highlightName.getFragments()[0].string();
                    itemDTO.setName(name);
                }
                itemDTOS.add(itemDTO);
            }
            result.setList(itemDTOS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return result;
        }

    }

    @Override
    public CategoryAndBrandDTO filter(ItemPageQuery query) {
        if(query == null){
            return null;
        }
        //连接客户端
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.249.130:9200")
        ));
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        //字段文本匹配查询
        String name = query.getKey();
        if(StrUtil.isNotBlank(name)){
            builder.must(QueryBuilders.matchQuery("name",name));
        }
        //品牌分类过滤
        String brand = query.getBrand();
        if(StrUtil.isNotBlank(brand)){
            builder.filter(QueryBuilders.termQuery("brand",brand));
        }
        //商品分类过滤
        String category = query.getCategory();
        if(StrUtil.isNotBlank(category)){
            builder.filter(QueryBuilders.termQuery("category",category));
        }

        String brandAgg = "brandAgg";
        String categoryAgg = "categoryAgg";
        //聚合查询品牌以及分类
        request.source().size(0)
                .query(builder)
                .aggregation(AggregationBuilders.terms(brandAgg).field("brand"))
                .aggregation(AggregationBuilders.terms(categoryAgg).field("category"));

        //解析数据
        List<String> brands = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            Aggregations aggregations = response.getAggregations();

            Terms brandBucket = aggregations.get(brandAgg);
            brandBucket.getBuckets().forEach(bucket -> {
                brands.add(bucket.getKeyAsString());
            });

            Terms categoryBucket = aggregations.get(categoryAgg);
            categoryBucket.getBuckets().forEach(bucket -> {
                categories.add(bucket.getKeyAsString());
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new CategoryAndBrandDTO(categories,brands);
    }
}
