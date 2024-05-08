const output = document.getElementById("output")!;

export const message = (message: string) => {
  const p = document.createElement("p");
  p.textContent += message;
  output.appendChild(p);
};

export const success = (message: string) => {
  const p = document.createElement("p");
  p.style.backgroundColor = "rgba(144, 234, 10, 0.467)";
  p.textContent += message;
  output.appendChild(p);
};

export const error = (message: string) => {
  const p = document.createElement("p");
  p.style.backgroundColor = "#f223";
  p.textContent += message;
  output.appendChild(p);
};
