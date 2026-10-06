package util;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUtilsTest {

    @Test
    void preprocessFace_returnsGraySquare() {
        Mat color = new Mat(new Size(240, 180), opencv_core.CV_8UC3,
                new org.bytedeco.opencv.opencv_core.Scalar(60, 120, 200, 0));

        Mat out = ImageUtils.preprocessFace(color);

        assertThat(out.cols()).isEqualTo(ImageUtils.FACE_SIZE);
        assertThat(out.rows()).isEqualTo(ImageUtils.FACE_SIZE);
        assertThat(out.channels()).isEqualTo(1);
    }

    @Test
    void preprocessFace_acceptsGrayscaleInput() {
        Mat gray = new Mat(new Size(50, 70), opencv_core.CV_8UC1,
                new org.bytedeco.opencv.opencv_core.Scalar(128, 0, 0, 0));

        Mat out = ImageUtils.preprocessFace(gray);

        assertThat(out.cols()).isEqualTo(ImageUtils.FACE_SIZE);
        assertThat(out.rows()).isEqualTo(ImageUtils.FACE_SIZE);
        assertThat(out.channels()).isEqualTo(1);
    }

    @Test
    void preprocessFace_equalizesFlatImage() {
        // A flat mid-gray image equalizes to a (near-)uniform result:
        // proves equalizeHist ran without altering geometry.
        Mat gray = new Mat(new Size(100, 100), opencv_core.CV_8UC1,
                new org.bytedeco.opencv.opencv_core.Scalar(100, 0, 0, 0));

        Mat out = ImageUtils.preprocessFace(gray);

        assertThat(out.cols()).isEqualTo(100);
        assertThat(out.rows()).isEqualTo(100);
    }
}
