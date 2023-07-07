import { X_PLAOC_QUERY } from "../server/const.ts";
import { RootComp } from "./index.html.ts";

const url = new URL(location.href);
url.searchParams.delete(X_PLAOC_QUERY.EMULATOR);

const app = new RootComp();
app.src = url.href;
document.body.append(app);
