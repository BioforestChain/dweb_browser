
export const ImageCapture = (videoTrack: MediaStreamTrack) => {
  let imageCapture = (window as any).ImageCapture
  if (imageCapture) {
    return new imageCapture(videoTrack) as ImageCapture
  }
  // Safari TODO
  class ImageCapture implements ImageCapture {
    private _blob = new Blob()
    constructor(readonly videoTrack: MediaStreamTrack) { }
    async takePhoto(): Promise<Blob> {
      this.videoTrack
      return this._blob
    }
  }
  return new ImageCapture(videoTrack)
}


interface ImageCapture {
  new(videoTrack: MediaStreamTrack);
  takePhoto(): Promise<Blob>
  // getPhotoCapabilities: Promise<PhotoCapabilities>
  // getPhotoSettings: Promise<PhotoSettings>
  // grabFrame(): Promise<ImageBitmap>
  // readonly attribute MediaStreamTrack track;
}
