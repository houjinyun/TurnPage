package com.hjy.turnpage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;
/**
 * 主要实现一个类似翻书的效果
 * 
 * @author Jinyun Hou
 * 
 */
public class TurnPageView extends View {
	private static final String TAG = "TurnPageView";
	
	private static DrawFilter  ANTI_ALIAS_FLAG = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
	private static int BG_COLOR = 0xFFAAAAAA;
	private static final int ANIM_DURATION = 3000;
	
	/** 书页的宽度 */
	private int mWidth;
	/** 书页的高度 */
	private int mHeight;
	/** 书页的对角线长度 */
	private float mDiagonalLength;

	/** 书页翻起的整个区域，包括当前页的背面部分与下一页的可见部分 */
	private Path mPath0;

	private Path mPath1;
	
	/** 拖拽点 */
	private PointF mTouch = new PointF();
	/** 拖拽点对应的页脚坐标X */
	private int mCornerX;
	/** 拖拽点对应的页脚坐标Y */
	private int mCornerY;
	
	/**
	 * 贝塞尔曲线的几个点
	 */
	private PointF mBezierStart1 = new PointF();		//贝塞尔曲线起始点
	private PointF mBezierControl1 = new PointF();		//贝塞尔曲线控制点
	private PointF mBezierVertex1 = new PointF();		//贝塞尔曲线定点
	private PointF mBezierEnd1 = new PointF();			//贝塞尔曲线结束点
	
	/**
	 * 另一条贝塞尔曲线
	 */
	private PointF mBezierStart2 = new PointF();		
	private PointF mBezierControl2 = new PointF();		
	private PointF mBezierVertex2 = new PointF();		
	private PointF mBezierEnd2 = new PointF();			
	
	/**
	 * 当前页
	 */
	private Bitmap mCurrPageBitmap;
	/**
	 * 下一页
	 */
	private Bitmap mNextPageBitmap;
	
	private Paint mPaint;
	
	/**
	 * 画当前页背面的时候进行翻转
	 */
	private Matrix mMatrix;
	float[] mMatrixArray;
	
	/**
	 * 画翻起页背面时进行颜色处理
	 */
	private ColorMatrixColorFilter mColorMatrixFilter;
	
	/**
	 * 翻转的方向，是否是在右上左下方向上进行翻页
	 */
	private boolean mIsRTandLB;
	
	private float mDegrees;
	
	/**
	 * 拖拽点到相应页脚点的距离
	 */
	private float mTouchToCornorPointDistance;
	
	private GradientDrawable mBackShadowDrawableLR;
	private GradientDrawable mBackShadowDrawableRL;
	
	private GradientDrawable mFrontShadowDrawableHBT;
	private GradientDrawable mFrontShadowDrawableHTB;
	private GradientDrawable mFrontShadowDrawableVLR;
	private GradientDrawable mFrontShadowDrawableVRL;
	
	private GradientDrawable mFolderShadowDrawableLR;
	private GradientDrawable mFolderShadowDrawableRL;
	
	/**
	 * 进行翻转动画
	 */
	private Scroller mScroller;
	
	public TurnPageView(Context context) {
		super(context);
		init();
	}
	
	public TurnPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public TurnPageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * 初始化参数
	 */
	private void init() {
		mPath0 = new Path();
		mPath1 = new Path();
		mPaint = new Paint();
		mMatrix = new Matrix();
		mMatrixArray = new float[]{ 0, 0, 0, 0, 0, 0, 0, 0, 1.0f };
		
		ColorMatrix cm = new ColorMatrix();
		float array[] = {
				0.55f, 0, 	  0, 	 0, 	80.0f, 
				0, 	   0.55f, 0, 	 0, 	80.0f,
				0, 	   0, 	  0.55f, 0, 	80.0f,
				0, 	   0, 	  0, 	 0.2f,  0 };
		cm.set(array);
		mColorMatrixFilter = new ColorMatrixColorFilter(cm);
		
		createShadowDrawables();
		mScroller = new Scroller(getContext());
		
		mTouch.x = 0.01f; // 不让x,y为0,否则在点计算时会有问题
		mTouch.y = 0.01f;
	}

	/**
	 * 创建绘制阴影所需的drawable
	 */
	private void createShadowDrawables() {
		int[] mBackShadowColors = new int[] { 0xff111111, 0x00111111 };
		mBackShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors);
		mBackShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		mBackShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
		mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		
		int[] mFrontShadowColors = new int[] { 0x80111111, 0x00111111 };
		mFrontShadowDrawableVLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors);
		mFrontShadowDrawableVLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		mFrontShadowDrawableVRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors);
		mFrontShadowDrawableVRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		mFrontShadowDrawableHTB = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors);
		mFrontShadowDrawableHTB.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		mFrontShadowDrawableHBT = new GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors);
		mFrontShadowDrawableHBT.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		
		int[] color = { 0x00333333, 0xb0333333 };
		mFolderShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, color);
		mFolderShadowDrawableRL
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFolderShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, color);
		mFolderShadowDrawableLR
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = w;
		mHeight = h;
		mDiagonalLength = (float)Math.hypot(w, h);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mTouch.x = x;
			mTouch.y = y;
			//1.如果动画还没有停止，则要abort
			abortAnimation();
			//2.计算拖拽点对应的页脚坐标
			calcCornerXY(x, y);
			//3.计算贝塞尔曲线点
			calcBezierPoints();
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			mTouch.x = x;
			mTouch.y = y;
			calcBezierPoints();
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if(canDragOver())
				startAnimation(ANIM_DURATION);
			else {
				mTouch.x = mCornerX - 0.09f;
				mTouch.y = mCornerY - 0.09f;
			}
			invalidate();
			break;
		}
		return true;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(mCurrPageBitmap == null)
			return;
		canvas.setDrawFilter(ANTI_ALIAS_FLAG);
		canvas.drawColor(BG_COLOR);
		drawCurrPage(canvas, mCurrPageBitmap);
		drawNextPageAndShadow(canvas, mNextPageBitmap);
		drawCurrPageShadow(canvas);
		drawCurrBackPage(canvas, mCurrPageBitmap);
	}
	
	/**
	 * 画当前页，要除去翻起的部分以及下一页能见的区域
	 * 
	 * @param canvas
	 */
	private void drawCurrPage(Canvas canvas, Bitmap bmp) {
		mPath0.reset();
		mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y);
		mPath0.lineTo(mTouch.x, mTouch.y);
		mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y);
		mPath0.lineTo(mCornerX, mCornerY);
		mPath0.close();
		canvas.save();
		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.drawBitmap(bmp, 0, 0 , null);
		canvas.restore();
	}

	/**
	 * 画下一页的可见区域，以及阴影区域
	 * 
	 * @param canvas
	 * @param bmp
	 */
	private void drawNextPageAndShadow(Canvas canvas, Bitmap bmp) {
		if(bmp == null)
			return;
		//先计算下一页可见区域的路径
		mPath1.reset();
		mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.lineTo(mBezierVertex1.x, mBezierVertex1.y);
		mPath1.lineTo(mBezierVertex2.x, mBezierVertex2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.lineTo(mCornerX, mCornerY);
		mPath1.close();

		//再计算书页翻起的阴影区域
		int shadowLeft;
		int shadowRight;
		GradientDrawable backShadowDrawable;
		if(mIsRTandLB) {
			shadowLeft = (int)(mBezierStart1.x);
			shadowRight = (int)(mBezierStart1.x + mTouchToCornorPointDistance/4);
			backShadowDrawable = mBackShadowDrawableLR;
		} else {
			shadowLeft = (int)(mBezierStart1.x - mTouchToCornorPointDistance/4);
			shadowRight = (int)(mBezierStart1.x);
			backShadowDrawable = mBackShadowDrawableRL;
		}
		mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x
				- mCornerX, mBezierControl2.y - mCornerY));
		backShadowDrawable.setBounds(shadowLeft, (int) mBezierStart1.y, shadowRight,
				(int) (mDiagonalLength + mBezierStart1.y));
		Log.d(TAG, "drawNextPage(): degress = " + mDegrees);
		
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		canvas.drawBitmap(bmp, 0, 0, null);
		
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		backShadowDrawable.draw(canvas);
		
		canvas.restore();
	}
	
	/**
	 * 画当前页的阴影
	 * 
	 * @param canvas
	 */
	public void drawCurrPageShadow(Canvas canvas) {
		double degree;
		if (mIsRTandLB) {
			degree = Math.PI / 4 - Math.atan2(mBezierControl1.y - mTouch.y, mTouch.x
							- mBezierControl1.x);
		} else {
			degree = Math.PI / 4 - Math.atan2(mTouch.y - mBezierControl1.y, mTouch.x
							- mBezierControl1.x);
		}
		Log.d(TAG, "degree = " + degree);
		
		// 翻起页阴影顶点与touch点的距离
		double d1 = (float) 25 * 1.414 * Math.cos(degree);
		double d2 = (float) 25 * 1.414 * Math.sin(degree);
		Log.d(TAG, "d1 = " + d1 + ",d2 = " + d2);
		float x = (float) (mTouch.x + d1);
		float y;
		if (mIsRTandLB) {
			y = (float) (mTouch.y + d2);
		} else {
			y = (float) (mTouch.y - d2);
		}
		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
		mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.close();
		
		float rotateDegrees;
		canvas.save();
		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		int leftx;
		int rightx;
		GradientDrawable currentPageShadow;
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl1.x);
			rightx = (int) mBezierControl1.x + 25;
			currentPageShadow = mFrontShadowDrawableVLR;
		} else {
			leftx = (int) (mBezierControl1.x - 25);
			rightx = (int) mBezierControl1.x + 1;
			currentPageShadow = mFrontShadowDrawableVRL;
		}

		rotateDegrees = (float) Math.toDegrees(Math.atan2(mTouch.x
				- mBezierControl1.x, mBezierControl1.y - mTouch.y));
		canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
		currentPageShadow.setBounds(leftx,
				(int) (mBezierControl1.y - mDiagonalLength), rightx,
				(int) (mBezierControl1.y));
		currentPageShadow.draw(canvas);
		canvas.restore();

		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.close();
		canvas.save();
		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl2.y);
			rightx = (int) (mBezierControl2.y + 25);
			currentPageShadow = mFrontShadowDrawableHTB;
		} else {
			leftx = (int) (mBezierControl2.y - 25);
			rightx = (int) (mBezierControl2.y + 1);
			currentPageShadow = mFrontShadowDrawableHBT;
		}
		rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y
				- mTouch.y, mBezierControl2.x - mTouch.x));
		canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
		float temp;
		if (mBezierControl2.y < 0)
			temp = mBezierControl2.y - mHeight;
		else
			temp = mBezierControl2.y;

		int hmg = (int) Math.hypot(mBezierControl2.x, temp);
		if (hmg > mDiagonalLength)
			currentPageShadow
					.setBounds((int) (mBezierControl2.x - 25) - hmg, leftx,
							(int) (mBezierControl2.x + mDiagonalLength) - hmg,
							rightx);
		else
			currentPageShadow.setBounds(
					(int) (mBezierControl2.x - mDiagonalLength), leftx,
					(int) (mBezierControl2.x), rightx);

		currentPageShadow.draw(canvas);
		canvas.restore();
	}
	
	/**
	 * 画当前页被翻起的背面部分以及阴影
	 * 
	 * @param canvas
	 * @param bmp
	 */
	private void drawCurrBackPage(Canvas canvas, Bitmap bmp) {
		mPath1.reset();
		mPath1.moveTo(mBezierVertex2.x, mBezierVertex2.y);
		mPath1.lineTo(mBezierVertex1.x, mBezierVertex1.y);
		mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath1.close();
		
		int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
		float f1 = Math.abs(i - mBezierControl1.x);
		int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
		float f2 = Math.abs(i1 - mBezierControl2.y);
		float f3 = Math.min(f1, f2);
		GradientDrawable folderShadowDrawable;
		int left;
		int right;
		if (mIsRTandLB) {
			left = (int) (mBezierStart1.x - 1);
			right = (int) (mBezierStart1.x + f3 + 1);
			folderShadowDrawable = mFolderShadowDrawableLR;
		} else {
			left = (int) (mBezierStart1.x - f3 - 1);
			right = (int) (mBezierStart1.x + 1);
			folderShadowDrawable = mFolderShadowDrawableRL;
		}
		
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		
		//需要求出 点(x,y)关于直线(mBezierControl1, mBezierControl2)的对称点坐标，
		//进而根据坐标变换求出当前页翻起的矩阵变换方程
		//假设直线(mBezierControl1, mBezierControl2)与X轴的夹角为α
		float distance = (float) Math.hypot(mCornerX - mBezierControl1.x,
				mBezierControl2.y - mCornerY);
		float cosα = (mCornerX - mBezierControl1.x) / distance;
		//注意android坐标系左上角才是原点(0, 0)
		float sinα = -(mCornerY - mBezierControl2.y) / distance;
		/*
		 * 由数学运算，计算点(x0,y0)关于直线 y=(tanα)x的对称点坐标(a, b)
		 * a = (cosα * cosα - sinα * sinα) * x0 + 2 * sinα * cosα * y0;
		 * b = 2 * sinα * cosα * x0 + (1 - 2 * cosα * cosα) * y0
		 * 根据矩阵变换得出下面结论
		 */
		mMatrixArray[0] = 1 - 2 * sinα * sinα;
		mMatrixArray[1] = 2 * sinα * cosα;
		mMatrixArray[3] = mMatrixArray[1];
		mMatrixArray[4] = 1 - 2 * cosα * cosα;
		mMatrix.reset();
		mMatrix.setValues(mMatrixArray);
		mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
		mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
		mPaint.setColorFilter(mColorMatrixFilter);
		canvas.drawBitmap(bmp, mMatrix, mPaint);
		mPaint.setColorFilter(null);
		
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		folderShadowDrawable.setBounds(left, (int) mBezierStart1.y, right,
				(int) (mBezierStart1.y + mDiagonalLength));
		folderShadowDrawable.draw(canvas);
		
		canvas.restore();
	}
	
	/**
	 * 根据touch down时点的坐标计算拖拽点对应的页脚坐标
	 * 
	 * @param x
	 * @param y
	 */
	private void calcCornerXY(float x, float y) {
		mCornerX = x <= mWidth/2 ? 0 : mWidth;
		mCornerY = y <= mHeight/2 ? 0 : mHeight;
		mIsRTandLB = ((mCornerX == 0 && mCornerY == mHeight) || (mCornerX == mWidth && mCornerY == 0)) ? true : false;
	}
	
	/**
	 * 根据拖拽点、页脚坐标来计算贝塞尔曲线里的各点坐标值
	 */
	private void calcBezierPoints() {
		float mMiddleX = (mTouch.x + mCornerX) / 2;
		float mMiddleY = (mTouch.y + mCornerY) / 2;
		
		//计算控制点
		mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
		mBezierControl1.y = mCornerY;
		mBezierControl2.x = mCornerX;
		mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
		
		//计算起始点
		mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2;
		mBezierStart1.y = mCornerY;
		
		/**
		 * 当mBezierStart1.x < 0或者mBezierStart1.x > 480时
		 * 如果继续翻页，会出现BUG故在此限制
		 * 
		 * 在这里是防止书页会从下往上 或者 从上往下 翻起来，因为我们假定书页的左边是装订起来连在一起的。
		 * 如果类似挂历之类的翻页，因为其上边是固定起来的，所以需要不同的限制
		 */
		if (mTouch.x > 0 && mTouch.x < mWidth) {
			if (mBezierStart1.x < 0 || mBezierStart1.x > mWidth) {
				Log.e(TAG, "adjust point again...");
				if (mBezierStart1.x < 0)
					mBezierStart1.x = mWidth - mBezierStart1.x;
				//其实就是重新计算出一个touch点来，使得计算出来的该touch点，对应的贝塞尔曲线起始点mBezierStart1.x >= 0
				//至于为什么要这样计算，尚待证明
				//to do list:
				//1.搞清楚这个算法的来由
				float f1 = Math.abs(mCornerX - mTouch.x);
				float f2 = mWidth * f1 / mBezierStart1.x;
				mTouch.x = Math.abs(mCornerX - f2);
	
				float f3 = Math.abs(mCornerX - mTouch.x) * Math.abs(mCornerY - mTouch.y) / f1;
				mTouch.y = Math.abs(mCornerY - f3);
	
				mMiddleX = (mTouch.x + mCornerX) / 2;
				mMiddleY = (mTouch.y + mCornerY) / 2;
	
				mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
				mBezierControl1.y = mCornerY;
				mBezierControl2.x = mCornerX;
				mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
		
				mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2;
			}
		}
		mBezierStart2.x = mCornerX;
		mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y) / 2;
		
		//计算终止点
		calcCrossPoint(mTouch, mBezierControl1, mBezierStart1, mBezierStart2, mBezierEnd1);
		calcCrossPoint(mTouch, mBezierControl2, mBezierStart1, mBezierStart2, mBezierEnd2);
		
		//计算顶点，顶点其实就是 由 起始点、终止点、控制点 三点组成的三角形的重心点
		mBezierVertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
		mBezierVertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;
		mBezierVertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
		mBezierVertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4;
		
		mTouchToCornorPointDistance = (float) Math.hypot((mTouch.x - mCornerX), (mTouch.y - mCornerY));
	}
	
	/**
	 * 计算直线p1p2与p3p4的交叉点坐标，运用数学运算求两条相交直线的交点
	 * 		直线Y1方程为: y = a1x + b1;
	 * 		直线Y2方程为: y = a2x + b2;
	 * 点p1, p2在直线Y1上，点p3, p4在直线Y2上
	 * 
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param p4
	 */
	private void calcCrossPoint(PointF p1, PointF p2, PointF p3, PointF p4, PointF dest) {
		float a1 = (p1.y - p2.y) / (p1.x - p2.x);
		float b1 = (p1.x * p2.y - p2.x * p1.y) / (p1.x - p2.x);
		float a2 = (p3.y - p4.y) / (p3.x - p4.x);
		float b2 = ((p3.x * p4.y) - (p4.x * p3.y)) / (p3.x - p4.x);
		float x = (b2 - b1) / (a1 - a2);
		float y = a1 * x + b1;
		dest.x = x;
		dest.y = y;
	}
	
	@Override
	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			float x = mScroller.getCurrX();
			float y = mScroller.getCurrY();
			mTouch.x = x;
			mTouch.y = y;
			Log.e(TAG, "computeScroll():" + x + "," + y);
			calcBezierPoints();
			postInvalidate();
		}
	}
	
	/**
	 * 开始翻转动画
	 */
	private void startAnimation(int duration) {
		int dx, dy;
		// dx 水平方向滑动的距离，负值会使滚动向左滚动
		// dy 垂直方向滑动的距离，负值会使滚动向上滚动
		if (mCornerX > 0) {
			dx = -(int) (mWidth + mTouch.x);
		} else {
			dx = (int) (mWidth - mTouch.x + mWidth);
		}
		if (mCornerY > 0) {
			dy = (int) (mHeight - mTouch.y);
		} else {
			dy = (int) (1 - mTouch.y); // 防止mTouch.y最终变为0
		}
		Log.d(TAG, "start anim:" + dx + "," + dy);
		mScroller.startScroll((int) mTouch.x, (int) mTouch.y, dx, dy,
				duration);
	}
	
	public boolean canDragOver() {
		if (mTouchToCornorPointDistance > mWidth / 10)
			return true;
		return false;
	}
	
	/**
	 * 终止动画
	 */
	private void abortAnimation() {
		if(!mScroller.isFinished())
			mScroller.abortAnimation();
	}
	
	public void setBitmaps(Bitmap currPage, Bitmap nextPage) {
		mCurrPageBitmap = currPage;
		mNextPageBitmap = nextPage;
	}
	
}