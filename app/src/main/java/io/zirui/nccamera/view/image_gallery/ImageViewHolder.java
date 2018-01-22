package io.zirui.nccamera.view.image_gallery;

import android.view.View;
import android.widget.ImageView;

import butterknife.BindView;
import io.zirui.nccamera.R;
import io.zirui.nccamera.view.base.BaseViewHolder;


public class ImageViewHolder extends BaseViewHolder {

    @BindView(R.id.shot_card_image) ImageView imageView;

    public ImageViewHolder(View itemView) {
        super(itemView);
    }
}
