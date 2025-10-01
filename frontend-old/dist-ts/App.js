import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from 'react';
import reactLogo from './assets/react.svg';
import './App.css';
function App() {
    const [count, setCount] = useState(0);
    return (_jsxs(_Fragment, { children: [_jsx("div", { children: _jsx("a", { href: "https://react.dev", target: "_blank", rel: "noreferrer", children: _jsx("img", { src: reactLogo, className: "logo react", alt: "React logo" }) }) }), _jsx("h1", { children: "Core Platform Frontend" }), _jsxs("div", { className: "card", children: [_jsxs("button", { onClick: () => setCount((count) => count + 1), children: ["count is ", count] }), _jsxs("p", { children: ["Edit ", _jsx("code", { children: "src/App.tsx" }), " and save to test build"] })] }), _jsx("p", { className: "read-the-docs", children: "Click on the React logo to learn more" })] }));
}
export default App;
