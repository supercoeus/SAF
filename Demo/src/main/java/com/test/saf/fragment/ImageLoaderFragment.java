package com.test.saf.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.test.saf.R;
import com.test.saf.activity.ImageDetailActivity;
import com.test.saf.adapter.DividerGridItemDecoration;
import com.test.saf.adapter.ImageLoaderAdapter;
import com.test.saf.app.BaseFragment;
import com.test.saf.domain.MMPicsResponse;

import java.io.IOException;
import java.util.List;

import cn.salesuite.saf.adapter.OnItemClickListener;
import cn.salesuite.saf.http.rest.RestClient;
import cn.salesuite.saf.http.rest.RestUtil;
import cn.salesuite.saf.http.rest.UrlBuilder;
import cn.salesuite.saf.inject.Injector;
import cn.salesuite.saf.inject.annotation.InjectView;
import cn.salesuite.saf.log.L;
import cn.salesuite.saf.rxjava.RxAsyncTask;
import cn.salesuite.saf.utils.Preconditions;

/**
 * Created by Tony Shen on 2016/11/22.
 */

public class ImageLoaderFragment extends BaseFragment {

    @InjectView
    RecyclerView recyclerview;

    @InjectView(id=R.id.menu_labels_right)
    FloatingActionMenu faMenu;

    @InjectView
    FloatingActionButton fab1;

    @InjectView
    FloatingActionButton fab2;

    MMPicsResponse respnose;

    int page = 1;

    ProgressDialog progDailog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_imageloader, container, false);
        Injector.injectInto(this, v);

        initViews();
        initData();

        return v;
    }

    private void initViews() {

        progDailog = ProgressDialog.show(mContext, "Loading", "Please wait...", true);
        progDailog.setCancelable(false);

        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                page++;

                // 封装url
                UrlBuilder urlBuilder = new UrlBuilder("http://www.tngou.net/tnfs/api/list");
                urlBuilder.parameter("page", page);
                String url = urlBuilder.buildUrl();

                newGetPicTask(url);

                if (faMenu.isOpened()) {
                    faMenu.close(true);
                }
            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (page<=1) {
                    toast("前一批无美女，老板要么换下一批？");

                    if (faMenu.isOpened()) {
                        faMenu.close(true);
                    }

                    return;
                }

                page--;

                // 封装url
                UrlBuilder urlBuilder = new UrlBuilder("http://www.tngou.net/tnfs/api/list");
                urlBuilder.parameter("page", page);
                String url = urlBuilder.buildUrl();

                newGetPicTask(url);

                if (faMenu.isOpened()) {
                    faMenu.close(true);
                }
            }
        });

        recyclerview.addOnItemTouchListener(new OnItemClickListener(recyclerview){

            @Override
            public void onItemClick(RecyclerView.ViewHolder holder, int position) {
                if (respnose != null && Preconditions.isNotBlank(respnose.tngou)) {
                    MMPicsResponse.Pic pic =respnose.tngou.get(position);
                    if (pic!=null && Preconditions.isNotBlank(pic.img)) {
                        Intent intent = new Intent(mContext, ImageDetailActivity.class);
                        intent.putExtra("image", pic.img);
                        startActivity(intent);
                        mContext.overridePendingTransition(0, 0);
                    }
                }
            }

            @Override
            public void onLongPress(RecyclerView.ViewHolder holder, int position) {

            }
        });
    }

    private void initData() {

        String url = "http://www.tngou.net/tnfs/api/news";
        newGetPicTask(url);
    }

    private void newGetPicTask(String url) {

        if (progDailog != null && !progDailog.isShowing()) {
            progDailog = ProgressDialog.show(mContext, "Loading", "Please wait...", true);
            progDailog.setCancelable(false);
        }

        new GetPicTask(url,progDailog)
                .success(new RxAsyncTask.SuccessHandler<String>() {
                    @Override
                    public void onSuccess(String content) {
                        try {
                            respnose = RestUtil.parseAs(MMPicsResponse.class, content);
                            if (respnose != null && Preconditions.isNotBlank(respnose.tngou)) {
                                recyclerview.setLayoutManager(new GridLayoutManager(mContext,2));
                                recyclerview.setAdapter(new ImageLoaderAdapter(mContext, respnose.tngou));
                                recyclerview.addItemDecoration(new DividerGridItemDecoration(mContext));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).failed(new RxAsyncTask.FailedHandler() {
            @Override
            public void onFail(Throwable e) {
                L.e(e.getMessage());
            }
        });
    }

    class GetPicTask extends RxAsyncTask<String> {

        private String apiUrl;

        public GetPicTask(String url,Dialog dialog) {
            super(dialog);
            this.apiUrl = url;
        }

        @Override
        public String onExecute() {
            return RestClient.get(apiUrl).body();
        }
    }
}