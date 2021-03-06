package io.zirui.nccamera.view.image_gallery;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.Preconditions;

import java.util.List;

import io.zirui.nccamera.R;
import io.zirui.nccamera.model.Shot;

public class ImageGalleryAdapter extends RecyclerView.Adapter{

    public List<Shot> data;
    private OnClickImageListener onClickImageListener;
    private final int screenWidth;
    private Context context;

    public List<Shot> selected_usersList;

    public ImageGalleryAdapter(Context context, List<Shot> data, List<Shot> selected_usersList, OnClickImageListener onClickImageListener){
        this.context = context;
        this.data = data;
        this.onClickImageListener = onClickImageListener;
        this.screenWidth = getScreenWidth(context);
        this.selected_usersList = selected_usersList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shot_card, parent, false);
        view.setMinimumHeight(screenWidth / 3);
        // view.getLayoutParams().height = screenWidth / 3;
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final Shot shot = data.get(position);
        ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
        RequestOptions req = new RequestOptions();
        req.diskCacheStrategy(DiskCacheStrategy.ALL);
        req.centerCrop();
        Glide.with(imageViewHolder.imageView.getContext())
                .load(shot.uri)
                .thumbnail(0.5f)
                .apply(req)
                .into(imageViewHolder.imageView);
        imageViewHolder.clickableCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickImageListener.onClick(holder.getAdapterPosition(), data);
            }
        });
        if(selected_usersList.contains(shot)){
            //imageViewHolder.clickableCover.getBackground().setAlpha(128);
            imageViewHolder.clickableCover.setBackgroundColor(ContextCompat.getColor(context, R.color.shot_dark_grey));
            imageViewHolder.check.setVisibility(View.VISIBLE);

        }else{
            //imageViewHolder.clickableCover.getBackground().setAlpha(0);
            imageViewHolder.clickableCover.setBackgroundColor(ContextCompat.getColor(context, R.color.shot_light_grey));
            imageViewHolder.check.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size() == 0 ? 0 : data.size() - 1;
    }

    // Display#getSize(Point)
    @SuppressWarnings("deprecation")
    private static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = Preconditions.checkNotNull(wm).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public interface OnClickImageListener{
        void onClick(int position, List<Shot> shots);
    }
}
