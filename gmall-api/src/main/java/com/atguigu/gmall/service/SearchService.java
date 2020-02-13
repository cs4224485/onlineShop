package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSku;

import java.util.List;

public interface SearchService {
    List<PmsSearchSku> list(PmsSearchParam pmsSearchParam);
}
