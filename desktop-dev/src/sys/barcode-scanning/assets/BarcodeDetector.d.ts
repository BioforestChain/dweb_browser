import { Point2D } from "https://esm.sh/v124/popmotion@11.0.5/lib/types.js";
declare global {
  enum BarcodeFormat {
    "aztec",
    "code_128",
    "code_39",
    "code_93",
    "codabar",
    "data_matrix",
    "ean_13",
    "ean_8",
    "itf",
    "pdf417",
    "qr_code",
    "unknown",
    "upc_a",
    "upc_e",
  }

  /** Config to create instance of {@code BarcodeDetector}. */
  interface BarcodeDetectorConfig {
    formats: Array<string>;
  }

  interface BarcodeDetectorResult {
    /**
     * A DOMRectReadOnly, which returns the dimensions of a rectangle
     * representing the extent of a detected barcode, aligned with the image.
     */
    boundingBox: DOMRectReadOnly;

    /**
     * The x and y co-ordinates of the four corner points of the detected
     * barcode relative to the image, starting with the top left and working
     * clockwise. This may not be square due to perspective distortions within
     * the image.
     */
    cornerPoints: Array<Point2D>;

    /** The detected barcode format. */
    format: BarcodeFormat;

    /** A String decoded from the barcode data. */
    rawValue: string;
  }

  class BarcodeDetector {
    constructor(config: BarcodeDetectorConfig);

    /** Async decoding API. */
    detect(source: ImageBitmapSource): Promise<BarcodeDetectorResult[]>;

    /** Returns list of supported formats as string. */
    static getSupportedFormats(): Array<string>;
  }
}

export {};
