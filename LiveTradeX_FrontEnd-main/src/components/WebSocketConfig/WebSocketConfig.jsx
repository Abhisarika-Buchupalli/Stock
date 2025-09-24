import React, { useEffect, useRef } from "react";
import SockJS from "sockjs-client";
import { over } from "stompjs";
import notify from "../SideComponents/Toastify";

const apiUrl = process.env.REACT_APP_BACKEND_SERVER_URL || "http://localhost:8085";

const WebSocketConfig = ({ wsStore, setWsStore, socketOpen }) => {
  const stompClientRef = useRef(null);

  const fetchInitialStocks = async () => {
    try {
      const res = await fetch(`${apiUrl}/userStocks/availableStocks`);
      const data = await res.json();
      setWsStore(prev => ({ ...prev, listAllStocks: data }));
    } catch (err) {
      console.error("Error fetching initial stocks:", err);
    }
  };

  const connect = () => {
    const Sock = new SockJS(`${apiUrl}/ws`);
    stompClientRef.current = over(Sock);
    stompClientRef.current.connect({}, onConnected, onError);
  };

  const disconnect = () => {
    if (stompClientRef.current) {
      stompClientRef.current.disconnect(() => {
        notify("Live updates server disconnected successfully");
      });
      stompClientRef.current = null;
    }
  };

  useEffect(() => {
    fetchInitialStocks(); // Fetch initial stocks on mount
  }, []);

  useEffect(() => {
    if (socketOpen === "connect") connect();
    else if (socketOpen === "close") disconnect();
  }, [socketOpen]);

  const onError = () => {
    notify(
      "Live Connection Failed, but you can still use the application without interruption :)",
      true
    );
  };

  const onConnected = () => {
    stompClientRef.current.subscribe("/stockUpdates", onMessageReceived);
    notify("Connected to Live updates server");
  };

  const onMessageReceived = (response) => {
    try {
      const res = JSON.parse(response.body);
      setWsStore({ listAllStocks: res.listAllStocks, userCount: res.userCount });
    } catch (err) {
      console.error("Error parsing WebSocket message:", err);
    }
  };

  return null;
};

export default WebSocketConfig;
