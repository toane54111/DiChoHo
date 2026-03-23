package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.CookbookRecipeAdapter;
import com.example.gomarket.model.CookbookRecipe;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookbookListFragment extends Fragment
        implements CookbookRecipeAdapter.OnRecipeActionListener {

    private static final String ARG_TAB_TYPE = "tab_type";

    private RecyclerView rvRecipes;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private TextView tvEmptyText;

    private CookbookRecipeAdapter adapter;
    private List<CookbookRecipe> recipes = new ArrayList<>();
    private ApiService apiService;
    private SessionManager session;
    private String tabType;

    public static CookbookListFragment newInstance(String tabType) {
        CookbookListFragment fragment = new CookbookListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAB_TYPE, tabType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tabType = getArguments() != null ? getArguments().getString(ARG_TAB_TYPE, "suggestions") : "suggestions";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cookbook_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getApiService(requireContext());
        session = new SessionManager(requireContext());

        rvRecipes = view.findViewById(R.id.rvRecipes);
        emptyState = view.findViewById(R.id.emptyState);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyText = view.findViewById(R.id.tvEmptyText);

        adapter = new CookbookRecipeAdapter(requireContext(), recipes, this);
        rvRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecipes.setAdapter(adapter);

        // Set empty text per tab
        switch (tabType) {
            case "suggestions":
                tvEmptyText.setText("Chưa có công thức gợi ý");
                break;
            case "community":
                tvEmptyText.setText("Chưa có công thức từ cộng đồng");
                break;
            case "personal":
                tvEmptyText.setText("Bạn chưa có công thức nào\nHãy tạo hoặc bấm tim để lưu!");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecipes();
    }

    private void loadRecipes() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        long userId = session.getUserId();
        Call<List<CookbookRecipe>> call;

        switch (tabType) {
            case "suggestions":
                call = apiService.getCookbookSuggestions(0);
                break;
            case "community":
                call = apiService.getCookbookCommunity(0, userId > 0 ? userId : null);
                break;
            case "personal":
                if (userId <= 0) {
                    progressBar.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                    return;
                }
                call = apiService.getCookbookPersonal(userId);
                break;
            default:
                return;
        }

        call.enqueue(new Callback<List<CookbookRecipe>>() {
            @Override
            public void onResponse(Call<List<CookbookRecipe>> call, Response<List<CookbookRecipe>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    recipes.clear();
                    recipes.addAll(response.body());
                    adapter.updateRecipes(recipes);
                }
                updateEmptyState();
            }

            @Override
            public void onFailure(Call<List<CookbookRecipe>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        emptyState.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
        rvRecipes.setVisibility(recipes.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onRecipeClick(CookbookRecipe recipe) {
        Intent intent = new Intent(requireContext(), CookbookRecipeDetailActivity.class);
        intent.putExtra("RECIPE_ID", recipe.getId());
        startActivity(intent);
    }

    @Override
    public void onLikeClick(CookbookRecipe recipe, int position) {
        long userId = session.getUserId();
        if (userId <= 0) return;

        apiService.toggleCookbookLike(recipe.getId(), userId)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            boolean liked = Boolean.TRUE.equals(response.body().get("liked"));
                            int count = ((Number) response.body().get("likeCount")).intValue();
                            recipe.setIsLikedByUser(liked);
                            recipe.setLikeCount(count);
                            adapter.notifyItemChanged(position);
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
    }
}
