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

public class SuggestionFragment extends Fragment implements RecipeCardAdapter.OnRecipeClickListener {

    private RecyclerView rvSuggestions;
    private RecipeCardAdapter adapter;
    private List<Recipe> recipes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suggestion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSuggestions = view.findViewById(R.id.rvSuggestions);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeCardAdapter(getContext(), recipes, this);
        rvSuggestions.setAdapter(adapter);

        loadSuggestions();
    }

    private void loadSuggestions() {
        // Demo data - sau này load từ API
        recipes.clear();
        recipes.add(createDemoRecipe("Sườn Xào Chua Ngọt", "Sườn heo xào sả ớt, vị chua ngọt đậm đà", 85000));
        recipes.add(createDemoRecipe("Canh Chua Cá Lóc", "Canh chua cá lóc nấu me, rau muống", 65000));
        recipes.add(createDemoRecipe("Thịt Kho Trứng", "Thịt heo kho trứng cút, nước dừa", 90000));
        recipes.add(createDemoRecipe("Gà Xào Nấm", "Gà xào nấm rơm, hành tây thơm", 95000));
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
