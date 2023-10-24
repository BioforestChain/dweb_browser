use std::fs;
use std::io;
use std::path::PathBuf;

fn decompress(zip_file_path: String, dest_path: String) -> u64 {
    let fname = std::path::Path::new(&zip_file_path);
    let file = fs::File::open(fname).unwrap();

    let mut archive = zip::ZipArchive::new(file).unwrap();

    for i in 0..archive.len() {
        let mut file = archive.by_index(i).unwrap();
        let _outpath = match file.enclosed_name() {
            Some(path) => path.to_owned(),
            None => continue,
        };

        let outpath = PathBuf::from(&dest_path).join(_outpath);

        if (*file.name()).ends_with('/') {
            fs::create_dir_all(&outpath).unwrap();
        } else {
            if let Some(p) = outpath.parent() {
                if !p.exists() {
                    fs::create_dir_all(p).unwrap();
                }
            }
            let mut outfile = fs::File::create(&outpath).unwrap();
            io::copy(&mut file, &mut outfile).unwrap();
        }
    }

    0
}

include!(concat!(env!("OUT_DIR"), "/ziplib.uniffi.rs"));
