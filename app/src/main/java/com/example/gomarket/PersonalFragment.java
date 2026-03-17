package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

public class PersonalFragment extends Fragment implements RecipeCardAdapter.OnRecipeClickListener {

    private RecyclerView rvPersonal;
    private RecipeCardAdapter adapter;
    private List<Recipe> recipes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPersonal = view.findViewById(R.id.rvPersonal);
        rvPersonal.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeCardAdapter(getContext(), recipes, this);
        rvPersonal.setAdapter(adapter);

        loadPersonalRecipes();
    }

    private void loadPersonalRecipes() {
        recipes.clear();
        recipes.add(createDemoRecipe("Món của tôi 1", "Công thức tự tạo", 75000));
        recipes.add(createDemoRecipe("Món yêu thích 1", "Đã tim ❤️", 60000));
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
