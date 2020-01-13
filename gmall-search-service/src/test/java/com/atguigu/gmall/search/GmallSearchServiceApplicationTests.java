package com.atguigu.gmall.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSku;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

    @Reference
    SkuService skuService; // 查询mysql

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
       put();
    }

    public void put() throws IOException {
        // 查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList = new ArrayList<>();
        pmsSkuInfoList= skuService.getAllSku("61");

        // 转化为es的数据结构
        List<PmsSearchSku> pmsSearchSkuInfos = new ArrayList<PmsSearchSku>();
        for (PmsSkuInfo skuInfo : pmsSkuInfoList) {
            PmsSearchSku pmsSearchSku = new PmsSearchSku();
            BeanUtils.copyProperties(skuInfo, pmsSearchSku);
            pmsSearchSku.setId(Long.parseLong(skuInfo.getId()));
            pmsSearchSkuInfos.add(pmsSearchSku);
        }
        // 导入es
        for (PmsSearchSku pmsSearchSkuInfo : pmsSearchSkuInfos) {
            Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall0105").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId() + "").build();
            jestClient.execute(put);
        }

    }

    public static  void query(){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        // filter
        TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("skuAttrValueList.valueId", "52");
        TermQueryBuilder termQueryBuilder2 = new TermQueryBuilder("skuAttrValueList.valueId", "49");
        boolQueryBuilder.filter(termQueryBuilder1);
        boolQueryBuilder.filter(termQueryBuilder2);
        // must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", "荣耀");
        boolQueryBuilder.must(matchQueryBuilder);
        // query
        searchSourceBuilder.query(boolQueryBuilder);
        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);
        // hi
    }
}
