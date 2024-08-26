package com.example.happyhunt.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.happyhunt.Util.DBHelper;
import com.example.happyhunt.Model.Favorite;
import com.example.happyhunt.databinding.PlaceFavoriteItemLayoutBinding;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List dataList;
    PlaceFavoriteItemLayoutBinding placeFavoriteItemBinding;
    boolean isSaved;
    DBHelper dbh;

    public FavoriteAdapter(List placesList, Context context) {
        super();
        this.dataList = placesList;
        this.dbh = new DBHelper(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        placeFavoriteItemBinding = PlaceFavoriteItemLayoutBinding.inflate(layoutInflater, parent, false);
        return new FavoriteAdapter.ViewHolder(placeFavoriteItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder). bindView(dataList, position);
    }
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        PlaceFavoriteItemLayoutBinding recyclerRowBinding;
        public ViewHolder(PlaceFavoriteItemLayoutBinding recyclerRowBinding) {
            super(recyclerRowBinding.getRoot());
            this.recyclerRowBinding = recyclerRowBinding;
        }

        public void bindView(List<Favorite> placeData, int position) {
            recyclerRowBinding.txtPlaceName.setText(placeData.get(position).getPlaceName());
            recyclerRowBinding.txtPlaceAddress.setText(placeData.get(position).getPlaceAddress());
        }
    }
}
