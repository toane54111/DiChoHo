package com.example.gomarket;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.gomarket.network.ApiClient;

import java.util.ArrayList;

public class SanPhamAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<SanPham> danhSachSanPham;

    // Constructor
    public SanPhamAdapter(Context context, ArrayList<SanPham> danhSachSanPham) {
        this.context = context;
        this.danhSachSanPham = danhSachSanPham;
    }

    @Override
    public int getCount() {
        return danhSachSanPham.size();
    }

    @Override
    public Object getItem(int position) {
        return danhSachSanPham.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // 1. Tạo View từ layout
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_san_pham, parent, false);
        }

        // 2. Lấy sản phẩm hiện tại
        SanPham sanPham = danhSachSanPham.get(position);

        // 3. Ánh xạ các View
        ImageView imgSanPham = convertView.findViewById(R.id.imgSanPham);
        TextView tvTenSanPham = convertView.findViewById(R.id.tvTenSanPham);
        TextView tvGiaGoc = convertView.findViewById(R.id.tvGiaGoc);
        TextView tvGiaKhuyenMai = convertView.findViewById(R.id.tvGiaKhuyenMai);

        // 4. Gán dữ liệu — ưu tiên ảnh URL từ API, fallback drawable local
        String imageUrl = ApiClient.getFullImageUrl(sanPham.getImageUrl());
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .apply(new RequestOptions()
                            .override(250, 250)                // Decode nhỏ hơn → nhanh hơn
                            .format(DecodeFormat.PREFER_RGB_565) // Dùng ít RAM hơn (16bit thay vì 32bit)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .centerCrop())
                    .thumbnail(0.1f)                           // Thumbnail nhỏ load trước
                    .placeholder(R.drawable.img)
                    .error(sanPham.getHinhAnh() != 0 ? sanPham.getHinhAnh() : R.drawable.img)
                    .into(imgSanPham);
        } else {
            imgSanPham.setImageResource(sanPham.getHinhAnh());
        }
        tvTenSanPham.setText(sanPham.getTenSanPham());
        tvGiaGoc.setText(sanPham.getGiaGoc());
        tvGiaKhuyenMai.setText(sanPham.getGiaKhuyenMai());

        // 5. Gạch ngang giá gốc
        tvGiaGoc.setPaintFlags(tvGiaGoc.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        return convertView;
    }
}