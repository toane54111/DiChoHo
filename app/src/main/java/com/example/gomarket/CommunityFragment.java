package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.RecipeCardAdapter;
import com.example.gomarket.model.Recipe;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment implements RecipeCardAdapter.OnRecipeClickListener {

    private RecyclerView rvCommunity;
    private RecipeCardAdapter adapter;
    private List<Recipe> recipes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCommunity = view.findViewById(R.id.rvCommunity);
        rvCommunity.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeCardAdapter(getContext(), recipes, this);
        rvCommunity.setAdapter(adapter);

        loadCommunityRecipes();
    }

    private void loadCommunityRecipes() {
        recipes.clear();
        recipes.add(createDemoRecipe("Bò Lúc Lắc", "Bò xào hành tây, ớt chuông", 180000));
        recipes.add(createDemoRecipe("Mực Xào Dưa", "Mực xào dưa cải chua", 95000));
        recipes.add(createDemoRecipe("Rau Muống Xào", "Rau muống xào tỏi", 25000));
        adapter.notifyDataSetChanged();
    }

    private Recipe createDemoRecipe(String name, String desc, double cost) {
        Recipe r = new Recipe();
        r.setId((int) (Math.random() * 1000));
        r.setName(name);
        r.setDescription(desc);
        r.setTotalCost(cost);
        return r;
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(getActivity(), RecipeDetailActivity.class);
        intent.putExtra("recipe_json", new Gson().toJson(recipe));
        startActivity(intent);
    }
}
